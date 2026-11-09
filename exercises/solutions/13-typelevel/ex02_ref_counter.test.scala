//> using scala 3.9.0
//> using dep org.typelevel::cats-effect:3.7.1
//> using test.dep org.scalameta::munit:1.3.6
//> using test.dep org.typelevel::munit-cats-effect:2.0.0
// See also: ecosystem/typelevel/03_cats_effect.scala

package solutions.typelevel.refcounter

import cats.effect.{IO, Ref}
import cats.syntax.all.*
import munit.CatsEffectSuite

// Task:
// Implement `def countConcurrently(fiberCount: Int, incrementsPerFiber: Int): IO[Int]`.
// Initialize an atomic `Ref[IO, Int]` at 0, launch `fiberCount` concurrent fibers
// using `parTraverse` or `parTraverse_` where each fiber increments the counter
// `incrementsPerFiber` times, wait for all fibers to complete, and return the final counter value.
// Example:
//   countConcurrently(10, 5) -> IO(50)

def countConcurrently(fiberCount: Int, incrementsPerFiber: Int): IO[Int] =
  for
    ref <- Ref.of[IO, Int](0)
    fiberWork = ref.update(_ + 1).replicateA_(incrementsPerFiber)
    _ <- (1 to fiberCount).toList.parTraverse_(_ => fiberWork)
    res <- ref.get
  yield res

class RefCounterSuite extends CatsEffectSuite:
  test("increments counter concurrently across multiple fibers") {
    assertIO(countConcurrently(10, 10), 100)
  }

  test("scales to 100 concurrent fibers") {
    assertIO(countConcurrently(100, 20), 2000)
  }

  test("handles zero fibers or zero increments cleanly") { // ⚠
    assertIO(countConcurrently(0, 50), 0)
    assertIO(countConcurrently(50, 0), 0)
  }
