//> using scala 3.9.0
//> using jvm 21
// Run: scala-cli run lessons/09-concurrency/03_virtual_threads.scala
// (Requires JDK 21+. The `using jvm 21` directive makes scala-cli fetch one if needed.)
//
// Virtual threads (Project Loom, JDK 21): cheap threads scheduled by the JVM onto a few
// carrier threads. Blocking a virtual thread is cheap, so plain "direct-style" blocking code
// scales like async code. This is the basis of Scala "direct style" libraries (Ox, gears).

package concurrency.loom

import java.util.concurrent.{Executors, Callable}
import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters.*
import scala.util.Using

def timed[A](label: String)(body: => A): A =
  val t0 = System.nanoTime()
  val r = body
  println(f"  $label%-45s ${(System.nanoTime() - t0) / 1e6}%8.1f ms")
  r

@main def run(): Unit =
  section("Starting virtual threads")
  val vt = Thread.ofVirtual().name("my-virtual").start(() =>
    println(s"  in ${Thread.currentThread()} isVirtual=${Thread.currentThread().isVirtual}")
  )
  vt.join()
  Thread.startVirtualThread(() => println("  startVirtualThread")).join()

  section("10 000 blocking tasks")
  val n = 10_000
  timed("virtual-thread-per-task executor") {
    Using.resource(Executors.newVirtualThreadPerTaskExecutor()) { exec =>
      val futures = (1 to n).map(i => exec.submit((() => { Thread.sleep(100); i }): Callable[Int]))
      futures.map(_.get).sum
    }
  }
  timed("fixed pool of 200 platform threads") {
    Using.resource(Executors.newFixedThreadPool(200)) { exec =>
      val futures = (1 to n).map(i => exec.submit((() => { Thread.sleep(100); i }): Callable[Int]))
      futures.map(_.get).sum
    }
  }
  // 10 000 × 100 ms sleeps: ~100 ms total with virtual threads vs ~5 s with 200 platform threads.

  section("Direct style: blocking code, concurrent execution")
  def fetchUser(id: Int): String = { Thread.sleep(100); s"user$id" }
  def fetchOrders(id: Int): List[String] = { Thread.sleep(100); List(s"order-$id-a") }
  timed("fork two blocking calls, join both") {
    Using.resource(Executors.newVirtualThreadPerTaskExecutor()) { exec =>
      val u = exec.submit((() => fetchUser(1)): Callable[String])
      val o = exec.submit((() => fetchOrders(1)): Callable[List[String]])
      println(s"  ${u.get} has ${o.get}")
    }
  }

  section("Pinning and other caveats")
  // * `synchronized` blocks used to PIN the carrier thread while blocked (JDK 21–23);
  //   JDK 24+ (JEP 491) removed most pinning. ReentrantLock never pinned.
  // * ThreadLocals work but cost memory per virtual thread — millions of them add up.
  // * Don't pool virtual threads; create one per task.
  // * CPU-bound work gains nothing: there are still only ~N cores.
  val counter = AtomicInteger()
  Using.resource(Executors.newVirtualThreadPerTaskExecutor()) { exec =>
    (1 to 1000).foreach(_ => exec.submit((() => counter.incrementAndGet()): Callable[Int]))
  }                                    // close() waits for all tasks
  println(s"  counter = ${counter.get}")

  section("Structured concurrency")
  // java.util.concurrent.StructuredTaskScope is a PREVIEW API (needs --enable-preview).
  // In Scala, use Ox (ecosystem/direct-style) or cats-effect/ZIO fibers for structured
  // concurrency with cancellation and supervision.
  println("  see ecosystem/direct-style/ox.scala")

def section(title: String): Unit = println(s"\n=== $title ===")
