//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
//> using test.dep org.typelevel::munit-cats-effect:2.0.0
//> using test.dep org.typelevel::cats-effect:3.7.1
// See also: ecosystem/typelevel/03_cats_effect.scala
// Run: scala-cli test ecosystem/testing/munit_cats_effect.test.scala
//
// MUnit integration with Cats Effect via CatsEffectSuite.
// Supports effectful tests returning IO, Resource fixtures, and error interception.

package ecosystem.testing.munitcatseffectdemo

import cats.effect.{IO, Ref, Resource}
import munit.CatsEffectSuite

class MUnitCatsEffectSuite extends CatsEffectSuite:

  // 1. IO that asserts a computed value
  test("IO asserts a computed value") {
    assertIO(IO(21 * 2), 42)
  }

  // 2. Resource fixture that acquires a value and releases it
  val fixture = ResourceSuiteLocalFixture(
    "managed-resource",
    Resource.make(Ref.of[IO, String]("active"))(ref => ref.set("released"))
  )

  override def munitFixtures = List(fixture)

  test("Resource fixture acquires a value and provides it to tests") {
    val ref = fixture()
    ref.get.flatMap(state => IO(assertEquals(state, "active")))
  }

  // 3. IO that fails with IllegalArgumentException which the test expects
  test("IO fails with expected IllegalArgumentException") {
    val failing = IO.raiseError[Int](new IllegalArgumentException("invalid input parameter"))
    interceptIO[IllegalArgumentException](failing).map { ex =>
      assertEquals(ex.getMessage, "invalid input parameter")
    }
  }
