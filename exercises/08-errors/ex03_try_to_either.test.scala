//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/08-errors/02_option_either_try.scala

package exercises.errors.trytoeither

import scala.util.Try

// Task:
// Define a domain error hierarchy:
//   enum AppError:
//     case Network(msg: String)
//     case Validation(msg: String)
//     case Unknown(msg: String)
// Implement `def safeCall[A](block: => A): Either[AppError, A]`:
//   Wrap `block` in `scala.util.Try` without catching fatal errors (VirtualMachineError, ThreadDeath, etc.).
//   Map exceptions:
//     - java.io.IOException -> AppError.Network(e.getMessage)
//     - IllegalArgumentException -> AppError.Validation(e.getMessage)
//     - Other non-fatal Throwables -> AppError.Unknown(e.getMessage)

enum AppError:
  case Network(msg: String)
  case Validation(msg: String)
  case Unknown(msg: String)

def safeCall[A](block: => A): Either[AppError, A] = ???

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
