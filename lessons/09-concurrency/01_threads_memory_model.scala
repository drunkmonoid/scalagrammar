//> using scala 3.9.0
// Run: scala-cli run lessons/09-concurrency/01_threads_memory_model.scala
//
// The JVM concurrency foundation that every Scala library sits on:
// threads, races, visibility (@volatile), synchronized, atomics, locks, executors,
// concurrent collections — and the classic bugs.

package concurrency.threads

import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, Executors, TimeUnit}
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference, LongAdder}
import java.util.concurrent.locks.ReentrantLock
import scala.jdk.CollectionConverters.*

def runInThreads(n: Int)(body: Int => Unit): Unit =
  val threads = (1 to n).map(i => Thread(() => body(i)))
  threads.foreach(_.start())
  threads.foreach(_.join())

@main def run(): Unit =
  section("Creating threads")
  val t = Thread(() => println(s"  hello from ${Thread.currentThread().getName}"))
  t.start()
  t.join()                                   // wait for it
  val daemon = Thread(() => Thread.sleep(10_000))
  daemon.setDaemon(true)                     // daemon threads don't keep the JVM alive
  daemon.start()

  section("Race condition: lost updates")
  var unsafeCounter = 0
  runInThreads(8)(_ => for _ <- 1 to 100_000 do unsafeCounter += 1)   // read-modify-write isn't atomic
  println(s"  unsafe counter: $unsafeCounter (expected 800000)")

  section("Fix 1: synchronized")
  var syncCounter = 0
  val lock = new Object
  runInThreads(8)(_ => for _ <- 1 to 100_000 do lock.synchronized { syncCounter += 1 })
  println(s"  synchronized counter: $syncCounter")

  section("Fix 2: atomics (lock-free CAS)")
  val atomic = AtomicInteger(0)
  runInThreads(8)(_ => for _ <- 1 to 100_000 do atomic.incrementAndGet())
  println(s"  AtomicInteger: ${atomic.get}")
  val adder = LongAdder()                    // better under heavy contention
  runInThreads(8)(_ => for _ <- 1 to 100_000 do adder.increment())
  println(s"  LongAdder: ${adder.sum}")
  // AtomicReference + immutable data = safe shared state
  val state = AtomicReference(List.empty[Int])
  runInThreads(4)(i => state.updateAndGet(i :: _))
  println(s"  AtomicReference[List]: ${state.get.sorted}")

  section("Visibility: @volatile")
  // Without @volatile the reader thread may never see `stop = true` (JIT hoists the read).
  Flags.stop = false
  val spinner = Thread { () =>
    var spins = 0L
    while !Flags.stop do spins += 1
    println(s"  spinner saw stop after $spins spins")
  }
  spinner.start()
  Thread.sleep(50)
  Flags.stop = true
  spinner.join(2000)
  println(s"  spinner alive after join? ${spinner.isAlive}")

  section("Explicit locks")
  val rl = ReentrantLock()
  var guarded = 0
  runInThreads(4)(_ =>
    for _ <- 1 to 10_000 do
      rl.lock()
      try guarded += 1
      finally rl.unlock()
  )
  println(s"  ReentrantLock counter: $guarded")
  println(s"  tryLock with timeout: ${rl.tryLock(10, TimeUnit.MILLISECONDS)}"); rl.unlock()

  section("Deadlock (avoided here with tryLock)")
  // Thread A locks L1 then L2; thread B locks L2 then L1 → both wait forever.
  // Cures: always acquire locks in a global order; use tryLock with timeouts; avoid locks.
  println("  see comment")

  section("Executors & thread pools")
  val pool = Executors.newFixedThreadPool(4)
  val latch = CountDownLatch(3)
  for i <- 1 to 3 do
    // execute(Runnable). `submit` is overloaded (Runnable / Callable), which can make a
    // Unit-returning lambda ambiguous.
    pool.execute(() =>
      println(s"  task $i on ${Thread.currentThread().getName}")
      latch.countDown()
    )
  latch.await()
  pool.shutdown()
  pool.awaitTermination(1, TimeUnit.SECONDS)
  // ⚠ forgetting shutdown() keeps non-daemon pool threads alive → the JVM never exits.

  section("Concurrent collections")
  val chm = ConcurrentHashMap[String, Int]()
  runInThreads(4)(i => for k <- 1 to 1000 do chm.merge(s"k${k % 10}", 1, _ + _))
  println(s"  ConcurrentHashMap total = ${chm.values.asScala.sum}")
  // scala.collection.concurrent.TrieMap: lock-free, with atomic snapshots
  val trie = scala.collection.concurrent.TrieMap.empty[Int, String]
  runInThreads(4)(i => trie.putIfAbsent(i % 2, s"thread $i"))
  println(s"  TrieMap: ${trie.size} keys")
  // ⚠ scala.collection.mutable.* is NOT thread-safe; even reads during writes can break.

  section("Immutability is the easiest concurrency strategy")
  val shared = Vector(1, 2, 3)               // safe to share across threads freely
  runInThreads(3)(i => require(shared.sum == 6))
  println("  immutable data needs no locks")

  section("ThreadLocal")
  val tl = ThreadLocal.withInitial(() => "default")
  runInThreads(2)(i => { tl.set(s"value-$i"); assert(tl.get == s"value-$i") })
  println(s"  main thread sees: ${tl.get}")

object Flags:
  @volatile var stop = false

def section(title: String): Unit = println(s"\n=== $title ===")
