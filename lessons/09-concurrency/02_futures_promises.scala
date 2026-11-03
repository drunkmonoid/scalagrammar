//> using scala 3.9.0
// Run: scala-cli run lessons/09-concurrency/02_futures_promises.scala
//
// scala.concurrent.Future: an eagerly-started, memoised, asynchronous computation.
// Promise: the writable side of a Future. ExecutionContext: where callbacks run.

package concurrency.futures

import scala.concurrent.{Await, ExecutionContext, Future, Promise, blocking}
import scala.concurrent.duration.*
import scala.util.{Failure, Success, Try}
import java.util.concurrent.Executors

given ExecutionContext = ExecutionContext.global   // a fork-join pool sized to the CPU count

def slow[A](ms: Long, label: String)(value: => A): Future[A] = Future {
  Thread.sleep(ms)
  println(s"  [${Thread.currentThread().getName}] $label done")
  value
}

@main def run(): Unit =
  section("Futures start immediately")
  val f = Future { println("  running eagerly"); 21 * 2 }
  println(Await.result(f, 1.second))          // ⚠ Await only at the edges (main, tests)

  section("Composition: map / flatMap / for")
  val sum = for
    a <- slow(100, "a")(1)
    b <- slow(100, "b")(2)                   // ⚠ SEQUENTIAL: b starts only after a completes
  yield a + b
  println(Await.result(sum, 2.seconds))
  // Start both first to run them in parallel:
  val fa = slow(100, "pa")(1)
  val fb = slow(100, "pb")(2)
  val parallel = for a <- fa; b <- fb yield a + b
  println(Await.result(parallel, 2.seconds))
  println(Await.result(fa.zip(fb), 1.second))
  println(Await.result(fa.zipWith(fb)(_ * _), 1.second))

  section("Collections of futures")
  val many = Future.sequence((1 to 5).map(i => slow(50, s"item $i")(i * i)))
  println(Await.result(many, 2.seconds))
  val traversed = Future.traverse(List("a", "bb", "ccc"))(s => Future(s.length))
  println(Await.result(traversed, 1.second))
  val first = Future.firstCompletedOf(List(slow(300, "slow")("slow"), slow(50, "fast")("fast")))
  println(Await.result(first, 1.second))
  println(Await.result(Future.foldLeft(List(Future(1), Future(2)))(0)(_ + _), 1.second))

  section("Failures")
  val boom: Future[Int] = Future(throw IllegalStateException("boom"))
  println(Await.ready(boom, 1.second).value)           // Some(Failure(...))
  val recovered = boom.recover { case _: IllegalStateException => -1 }
  println(Await.result(recovered, 1.second))
  val fallback = boom.recoverWith { case _ => Future.successful(0) }
  println(Await.result(fallback, 1.second))
  println(Await.result(boom.fallbackTo(Future.successful(7)), 1.second))
  println(Await.result(boom.transform {
    case Success(v) => Success(s"ok $v")
    case Failure(e) => Success(s"handled ${e.getMessage}")
  }, 1.second))
  try Await.result(boom, 1.second)
  catch case e: IllegalStateException => println(s"  Await.result rethrows: $e")
  // ⚠ filter / collect failures are NoSuchElementException:
  println(Await.ready(Future(1).filter(_ > 5), 1.second).value)

  section("Callbacks (side effects; prefer composition)")
  val done = Promise[Unit]()
  Future(42).onComplete {
    case Success(v) => println(s"  callback got $v"); done.success(())
    case Failure(e) => done.failure(e)
  }
  Await.ready(done.future, 1.second)

  section("Promises: completing a Future by hand")
  val p = Promise[String]()
  val pf = p.future
  Future { Thread.sleep(50); p.success("delivered") }
  println(Await.result(pf, 1.second))
  println(p.trySuccess("again"))                        // false — already completed
  try p.success("twice") catch case e: IllegalStateException => println(s"  $e")
  // Adapting a callback API:
  def legacyApi(cb: Either[Throwable, Int] => Unit): Unit = cb(Right(99))
  def modern: Future[Int] =
    val pr = Promise[Int]()
    legacyApi(r => pr.complete(r.toTry))
    pr.future
  println(Await.result(modern, 1.second))

  section("Timeouts")
  try Await.result(slow(500, "too slow")(1), 100.millis)
  catch case e: java.util.concurrent.TimeoutException => println(s"  timed out: ${e.getMessage}")
  // ⚠ the future keeps running! Futures are NOT cancellable.

  section("Execution contexts")
  val custom = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))
  val onCustom = Future(Thread.currentThread().getName)(using custom)
  println(s"  ran on ${Await.result(onCustom, 1.second)}")
  custom.shutdown()
  // ExecutionContext.parasitic runs callbacks on the completing thread (cheap, for tiny maps)
  println(Await.result(Future.successful(1).map(_ + 1)(using ExecutionContext.parasitic), 1.second))
  // Blocking inside the global pool: wrap with `blocking` so it can add threads
  val blockers = Future.traverse((1 to 20).toList)(i => Future(blocking { Thread.sleep(50); i }))
  println(s"  20 blocking tasks summed to ${Await.result(blockers, 5.seconds).sum}")

  section("Pitfalls")
  // 1. A Future is eager and memoised: `val f = Future(sideEffect)` runs once, right now.
  //    Referential transparency is lost: `val x = Future(...); (x, x)` ≠ `(Future(...), Future(...))`.
  // 2. for-comprehensions over futures created inside the for are sequential.
  // 3. Exceptions thrown in onComplete callbacks are reported to the EC, not to you.
  // 4. Never block (Await) inside a Future running on a small pool → thread starvation/deadlock.
  // Effect systems (cats-effect IO, ZIO) fix 1–3 with LAZY, cancellable effects. See ecosystem/.
  val counter = java.util.concurrent.atomic.AtomicInteger()
  val eager = Future(counter.incrementAndGet())
  Await.ready(eager.zip(eager), 1.second)
  println(s"  side effect ran ${counter.get} time(s) despite using the future twice")

def section(title: String): Unit = println(s"\n=== $title ===")
