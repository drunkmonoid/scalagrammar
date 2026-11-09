//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/09-concurrency/02_futures_promises.scala

package solutions.concurrency.firstsuccess

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.Await
import scala.util.{Failure, Success}
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

def firstSuccess[A](fs: List[Future[A]])(using ExecutionContext): Future[A] =
  if fs.isEmpty then
    Future.failed(new NoSuchElementException("firstSuccess called on empty list"))
  else
    val p = Promise[A]()
    val remaining = new AtomicInteger(fs.length)
    val lastError = new AtomicReference[Throwable]()

    for f <- fs do
      f.onComplete {
        case Success(value) =>
          p.trySuccess(value)
        case Failure(ex) =>
          lastError.set(ex)
          if remaining.decrementAndGet() == 0 then
            p.tryFailure(Option(lastError.get()).getOrElse(new NoSuchElementException("all futures failed")))
      }
    p.future

class FirstSuccessSuite extends munit.FunSuite:
  test("returns first successful future despite preceding failures") {
    val f1 = Future.failed[Int](new RuntimeException("boom1"))
    val f2 = Future.successful(42)
    val f3 = Future.failed[Int](new RuntimeException("boom2"))
    val result = Await.result(firstSuccess(List(f1, f2, f3)), 5.seconds)
    assertEquals(result, 42)
  }

  test("fails only when every future fails") { // ⚠
    val f1 = Future.failed[String](new IllegalArgumentException("err1"))
    val f2 = Future.failed[String](new IllegalStateException("err2"))
    val fut = firstSuccess(List(f1, f2))
    intercept[Exception] {
      Await.result(fut, 5.seconds)
    }
  }

  test("empty list fails with NoSuchElementException") {
    val fut = firstSuccess[Int](Nil)
    intercept[NoSuchElementException] {
      Await.result(fut, 5.seconds)
    }
  }
