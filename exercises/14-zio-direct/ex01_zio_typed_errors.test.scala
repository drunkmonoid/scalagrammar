//> using scala 3.9.0
//> using dep dev.zio::zio:2.1.27
//> using test.dep dev.zio::zio-test:2.1.27
//> using test.dep dev.zio::zio-test-sbt:2.1.27
// See also: ecosystem/zio/01_zio.scala

package exercises.ziodirect.ziotypederrors

import zio.*
import zio.test.*

// Task:
// Implement a ZIO program with typed errors using an `enum`:
// - `safeDivide(a: Double, b: Double): ZIO[Any, MathError, Double]`
//   Fails with `MathError.DivisionByZero` if `b == 0.0`, otherwise succeeds with `a / b`.
// - `safeSqrt(x: Double): ZIO[Any, MathError, Double]`
//   Fails with `MathError.NegativeSquareRoot(x)` if `x < 0.0`, otherwise succeeds with `math.sqrt(x)`.
// - `divideAndSqrt(a: Double, b: Double): ZIO[Any, MathError, Double]`
//   Composes `safeDivide` and `safeSqrt` in a for-comprehension.
// Example:
//   divideAndSqrt(32.0, 2.0) -> succeeds with 4.0
//   divideAndSqrt(10.0, 0.0) -> fails with MathError.DivisionByZero

enum MathError:
  case DivisionByZero
  case NegativeSquareRoot(value: Double)

def safeDivide(a: Double, b: Double): ZIO[Any, MathError, Double] = ???

def safeSqrt(x: Double): ZIO[Any, MathError, Double] = ???

def divideAndSqrt(a: Double, b: Double): ZIO[Any, MathError, Double] = ???

object ZioTypedErrorsSuite extends ZIOSpecDefault:
  def spec = suite("ZioTypedErrorsSuite")(
    test("succeeds with valid quotient and square root") {
      for
        res <- divideAndSqrt(32.0, 2.0)
      yield assertTrue(res == 4.0)
    },
    test("fails with DivisionByZero when divisor is zero") {
      for
        exit <- divideAndSqrt(10.0, 0.0).exit
      yield assertTrue(exit == Exit.fail(MathError.DivisionByZero))
    },
    test("fails with NegativeSquareRoot when quotient is negative") { // ⚠
      for
        exit <- divideAndSqrt(-16.0, 1.0).exit
      yield assertTrue(exit == Exit.fail(MathError.NegativeSquareRoot(-16.0)))
    }
  )
