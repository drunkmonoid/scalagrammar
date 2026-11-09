//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/08-errors/02_option_either_try.scala

package solutions.errors.trytoeither

import scala.util.{Failure, Success, Try}
import java.io.IOException

enum AppError:
  case Network(msg: String)
  case Validation(msg: String)
  case Unknown(msg: String)

def safeCall[A](block: => A): Either[AppError, A] =
  Try(block) match
    case Success(a)                          => Right(a)
    case Failure(e: IOException)             => Left(AppError.Network(e.getMessage))
    case Failure(e: IllegalArgumentException) => Left(AppError.Validation(e.getMessage))
    case Failure(e)                          => Left(AppError.Unknown(e.getMessage))

class TryToEitherSuite extends munit.FunSuite:
  test("returns Right on successful block evaluation") {
    val res = safeCall { 21 * 2 }
    assertEquals(res, Right(42))
  }

  test("maps specific exceptions to typed domain errors") {
    val net = safeCall[String] { throw new java.io.IOException("connect failed") }
    assertEquals(net, Left(AppError.Network("connect failed")))

    val valid = safeCall[Int] { throw new IllegalArgumentException("invalid id") }
    assertEquals(valid, Left(AppError.Validation("invalid id")))

    val other = safeCall[Unit] { throw new IllegalStateException("bad state") }
    assertEquals(other, Left(AppError.Unknown("bad state")))
  }

  test("does not catch fatal errors such as OutOfMemoryError") { // ⚠
    intercept[OutOfMemoryError] {
      safeCall { throw new OutOfMemoryError("fatal simulated OOM") }
    }
  }
