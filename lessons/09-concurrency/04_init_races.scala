//> using scala 3.9.0
// See also: lessons/02-functions/04_by_name_laziness.scala
// See also: lessons/03-oop/02_objects_companions.scala
// Run: scala-cli run lessons/09-concurrency/04_init_races.scala
//
// Initialization races and concurrency semantics in Scala 3:
// 1. Thread-safe single evaluation of `lazy val` under concurrent contention (16 threads)
// 2. Classloader lock deadlocks with mutually recursive singleton objects (daemon timeout)
// 3. Re-attempting failed `lazy val` initialization after exceptions

package concurrency.initraces

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch

// Target 1: Lazy val under 16-thread contention
class ConcurrentLazyHolder:
  val initRuns = AtomicInteger(0)
  lazy val payload: String =
    initRuns.incrementAndGet()
    Thread.sleep(10)
    "computed-payload"

// Target 2: Mutually recursive top-level objects that deadlock on class initialization locks
object ObjectPing:
  Thread.sleep(50) // allow ObjectPong thread to enter its class initializer
  val pongRef: Int = ObjectPong.pingRef + 1

object ObjectPong:
  Thread.sleep(50) // allow ObjectPing thread to enter its class initializer
  val pingRef: Int = ObjectPing.pongRef + 1

// Target 3: Lazy val that throws on first attempt and retries on subsequent accesses
class TransientFailingLazy:
  val attempts = AtomicInteger(0)
  lazy val result: String =
    val count = attempts.incrementAndGet()
    if count == 1 then
      throw new IllegalStateException("Transient init failure on attempt 1")
    else
      "recovered-value"

@main def run(): Unit =
  section("1. Concurrent lazy val initialization across 16 threads")
  val holder = ConcurrentLazyHolder()
  val startGate = CountDownLatch(1)
  val finishGate = CountDownLatch(16)

  val threads = (1 to 16).map { _ =>
    val t = Thread(() => {
      startGate.await()
      val v = holder.payload
      assert(v == "computed-payload")
      finishGate.countDown()
    })
    t.start()
    t
  }

  startGate.countDown()
  finishGate.await()
  val totalRuns = holder.initRuns.get()
  println(s"16 threads read lazy val concurrently. Initializer ran: $totalRuns time(s)")
  assert(totalRuns == 1)

  section("2. Object initialization deadlock across threads")
  // Two threads touch ObjectPing and ObjectPong simultaneously.
  // Each thread holds the initialization lock for one object while waiting for the other,
  // triggering a classic JVM classloader lock deadlock.
  // We run both as daemon threads so they do not block process termination.
  val pingThread = Thread(() => {
    val _ = ObjectPing.pongRef
  })
  pingThread.setDaemon(true)
  pingThread.setName("deadlock-ping-thread")

  val pongThread = Thread(() => {
    val _ = ObjectPong.pingRef
  })
  pongThread.setDaemon(true)
  pongThread.setName("deadlock-pong-thread")

  pingThread.start()
  pongThread.start()

  // Wait up to 2 seconds for both threads
  pingThread.join(2000)
  pongThread.join(2000)

  val pingAlive = pingThread.isAlive
  val pongAlive = pongThread.isAlive
  println(s"Deadlock observed: pingThread alive=$pingAlive, pongThread alive=$pongAlive")
  assert(pingAlive && pongAlive, "Expected both daemon threads to be deadlocked and still alive after 2s")

  section("3. Lazy val failure and retry semantics")
  val failingHolder = TransientFailingLazy()

  val firstFailed = try
    failingHolder.result
    false
  catch
    case _: IllegalStateException => true

  println(s"First access failed with expected exception: $firstFailed (attempts: ${failingHolder.attempts.get()})")
  assert(firstFailed)
  assert(failingHolder.attempts.get() == 1)

  // A second access from another thread retries initialization:
  var secondResult = ""
  val retryThread = Thread(() => {
    secondResult = failingHolder.result
  })
  retryThread.start()
  retryThread.join(2000)

  println(s"Second access succeeded on retry: '$secondResult' (total attempts: ${failingHolder.attempts.get()})")
  assert(secondResult == "recovered-value")
  assert(failingHolder.attempts.get() == 2)

def section(title: String): Unit = println(s"\n=== $title ===")
