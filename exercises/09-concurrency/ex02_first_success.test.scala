//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/09-concurrency/02_futures_promises.scala

package exercises.concurrency.firstsuccess

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.Await

// Task:
// Implement `firstSuccess[A](fs: List[Future[A]])(using ExecutionContext): Future[A]` built on a `Promise`.
// It completes with the value of the FIRST Future that completes successfully.
// ⚠ The returned Future fails ONLY when EVERY future in `fs` has failed (or if `fs` is empty).

def firstSuccess[A](fs: List[Future[A]])(using ExecutionContext): Future[A] = ???

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
