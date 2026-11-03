//> using scala 3.9.0
//> using jvm 21
//> using dep com.softwaremill.ox::core:1.1.0
// Run: scala-cli run ecosystem/direct-style/ox.scala
//
// "Direct style" Scala: write plain, blocking-looking code and let JDK 21 virtual threads make
// blocking cheap. Ox (by SoftwareMill) adds STRUCTURED CONCURRENCY on top:
//   * every fork lives inside a scope (supervised { ... }); the scope ends only when all its
//     forks are done, and a failing fork cancels its siblings — no leaked threads
//   * high-level combinators: par, raceSuccess, timeout, retry
//   * channels (Go-style) and Flows (lazy streams) for message passing
// Compare with the monadic style of cats-effect / ZIO in the other folders.

package ecosystem.ox

import ox.*
import ox.channels.*
import ox.flow.Flow
import ox.resilience.*
import ox.scheduling.Schedule
import scala.concurrent.duration.*

def fetchUser(id: Int): String =
  sleep(100.millis)                       // a blocking call: cheap on a virtual thread
  s"user-$id"

def fetchOrders(id: Int): List[String] =
  sleep(150.millis)
  List(s"order-$id-a", s"order-$id-b")

def timed[A](label: String)(body: => A): A =
  val t0 = System.nanoTime()
  val r = body
  println(f"  $label%-40s ${(System.nanoTime() - t0) / 1_000_000}%4d ms → $r")
  r

@main def run(): Unit =
  println("=== Structured concurrency with supervised + fork ===")
  timed("sequential") { (fetchUser(1), fetchOrders(1)) }
  timed("forked in a supervised scope") {
    supervised {
      val u = fork(fetchUser(1))
      val o = fork(fetchOrders(1))
      (u.join(), o.join())
    }
  }

  println("\n=== High-level combinators ===")
  timed("par") { par(fetchUser(2), fetchOrders(2)) }
  timed("raceSuccess (loser is interrupted)") { raceSuccess({ sleep(1.second); "slow" }, { sleep(50.millis); "fast" }) }
  timed("timeoutOption") { timeoutOption(100.millis)(sleep(1.second)) }
  timed("parallel map, 3 at a time") { (1 to 6).toList.mapPar(3)(i => { sleep(100.millis); i * i }) }

  println("\n=== A failing fork cancels its siblings ===")
  // `fork` creates a DAEMON fork: the scope interrupts it as soon as the body returns.
  // `forkUser` creates a USER fork: the scope waits for it before it can complete.
  // Either way, if any fork FAILS, the scope interrupts the others and rethrows.
  val outcome =
    try
      supervised {
        forkUser { sleep(2.seconds); println("  never printed: interrupted") }
        forkUser { sleep(50.millis); throw RuntimeException("fork failed") }
        "the body finished; the scope still waits for the user forks"
      }
    catch case e: RuntimeException => s"scope failed after ~50 ms: ${e.getMessage}"
  println(s"  $outcome")

  println("\n=== Retries ===")
  var attempts = 0
  val result = retry(Schedule.fixedInterval(20.millis).maxRetries(5)) {
    attempts += 1
    if attempts < 3 then throw RuntimeException(s"attempt $attempts failed") else s"ok after $attempts attempts"
  }
  println(s"  $result")

  println("\n=== Channels (Go-style) ===")
  supervised {
    val ch = Channel.buffered[Int](2)
    fork {
      for i <- 1 to 5 do ch.send(i)       // blocks when the buffer is full (back-pressure)
      ch.done()
    }
    val received = Iterator.continually(ch.receiveOrClosed()).takeWhile(_ != ChannelClosed.Done).toList
    println(s"  received $received")
  }

  println("\n=== Flows: lazy, back-pressured streams ===")
  val squares = Flow.iterate(1)(_ + 1).map(i => i * i).filter(_ % 2 == 1).take(5).runToList()
  println(s"  odd squares: $squares")
  val parallel = Flow.fromValues(1, 2, 3, 4).mapPar(2)(i => { sleep(50.millis); i * 10 }).runToList()
  println(s"  mapPar keeps order: $parallel")
