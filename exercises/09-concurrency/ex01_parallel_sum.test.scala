//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/09-concurrency/02_futures_promises.scala

package exercises.concurrency.parallelsum

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.Await

// Task:
// Implement `parallelSum(v: Vector[Long], k: Int)(using ExecutionContext): Future[Long]`.
// Split vector `v` into `k` chunks, sum each in a Future, and combine the chunk sums.
// If vector is empty, return Future.successful(0L).

def parallelSum(v: Vector[Long], k: Int)(using ExecutionContext): Future[Long] = ???

class ParallelSumSuite extends munit.FunSuite:
  test("computes sum of elements in parallel chunks") {
    val vec = (1L to 1000L).toVector
    val fut = parallelSum(vec, 4)
    val sum = Await.result(fut, 5.seconds)
    assertEquals(sum, 500500L)
  }

  test("handles empty vector and single-element vector") {
    val emptyFut = parallelSum(Vector.empty[Long], 2)
    assertEquals(Await.result(emptyFut, 5.seconds), 0L)

    val singleFut = parallelSum(Vector(42L), 5)
    assertEquals(Await.result(singleFut, 5.seconds), 42L)
  }

  test("handles chunk count larger than vector size") { // ⚠
    val vec = Vector(10L, 20L, 30L)
    val fut = parallelSum(vec, 100)
    assertEquals(Await.result(fut, 5.seconds), 60L)
  }
