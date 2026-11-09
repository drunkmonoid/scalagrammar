//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/06-types/10_opaque_types.scala

package solutions.types.meters

opaque type Meters = Double

object Meters:
  def apply(d: Double): Meters = d

extension (m: Meters)
  def + (other: Meters): Meters = m + other
  def toFeet: Double = m * 3.28084

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
