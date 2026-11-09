//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/06-types/10_opaque_types.scala

package exercises.types.meters

// Task:
// Define an opaque type `Meters` wrapping a `Double`:
//   opaque type Meters = Double
// Provide:
//   - A constructor `Meters(d: Double): Meters` in its companion object.
//   - An extension method `+ (other: Meters): Meters` adding two distances.
//   - An extension method `toFeet: Double` converting meters to feet (1 meter = 3.28084 feet).
// A `compileErrors` test asserts that `val m: Meters = 1.0` does not compile outside the opaque definition.
// Example: Meters(10.0) + Meters(5.0) -> Meters(15.0), Meters(1.0).toFeet -> 3.28084

opaque type Meters = Double

object Meters:
  def apply(d: Double): Meters = ???

extension (m: Meters)
  def + (other: Meters): Meters = ???
  def toFeet: Double = ???

class MetersSuite extends munit.FunSuite:
  test("adds two Meters distances") {
    val m1 = Meters(12.5)
    val m2 = Meters(7.5)
    val sum = m1 + m2
    assertEquals(sum.toFeet, (20.0 * 3.28084))
  }

  test("converts meters to feet") {
    val m = Meters(2.0)
    assertEquals(m.toFeet, 2.0 * 3.28084)
  }

  test("raw Double cannot be assigned to Meters") { // ⚠
    assert(compileErrors("val m: Meters = 1.0").nonEmpty)
  }
