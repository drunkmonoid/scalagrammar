//> using scala 3.9.0
//> using dep dev.zio::zio:2.1.27
//> using test.dep dev.zio::zio-test:2.1.27
//> using test.dep dev.zio::zio-test-sbt:2.1.27
// See also: ecosystem/zio/01_zio.scala
// Run: scala-cli test ecosystem/zio/02_zio_test.test.scala
//
// ZIO Test: effectful, purely functional testing with ZIOSpecDefault and assertTrue.

package ecosystem.ziodemo.test

import zio.*
import zio.test.*

// Service for layer testing
final case class ConfigService(appName: String)

object ConfigService:
  val live: ULayer[ConfigService] = ZLayer.succeed(ConfigService("ScalagrammarZIO"))

// Typed error for failure assertion
enum CalculationError:
  case DivisionByZero

def divide(a: Int, b: Int): IO[CalculationError, Int] =
  if b == 0 then ZIO.fail(CalculationError.DivisionByZero)
  else ZIO.succeed(a / b)

object ZioTestSpec extends ZIOSpecDefault:
  def spec = suite("ZIO Test suite")(
    // 1. assertTrue on a pure value
    test("pure value assertion with assertTrue") {
      val result = 20 + 22
      assertTrue(result == 42)
    },

    // 2. test that provides a ZLayer
    test("service dependency provided via ZLayer") {
      for
        service <- ZIO.service[ConfigService]
      yield assertTrue(service.appName == "ScalagrammarZIO")
    }.provide(ConfigService.live),

    // 3. effect that fails with a typed error
    test("typed error assertion") {
      for
        exit <- divide(10, 0).exit
      yield assertTrue(exit == Exit.fail(CalculationError.DivisionByZero))
    }
  )
