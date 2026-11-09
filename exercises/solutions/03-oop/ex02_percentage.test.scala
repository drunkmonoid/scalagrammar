//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/03-oop/04_case_classes.scala

package solutions.oop.percentage

case class Percentage private (value: Double)

object Percentage:
  def from(d: Double): Either[String, Percentage] =
    if d >= 0.0 && d <= 100.0 then Right(new Percentage(d))
    else Left(s"Invalid percentage: $d")

class PercentageSuite extends munit.FunSuite:
  test("Percentage.from validates valid percentages") {
    val p = Percentage.from(75.5)
    assert(p.isRight)
    assertEquals(p.map(_.value), Right(75.5))
    assertEquals(Percentage.from(0.0).map(_.value), Right(0.0))
    assertEquals(Percentage.from(100.0).map(_.value), Right(100.0))
  }

  test("Percentage.from rejects numbers outside [0.0, 100.0]") { // ⚠
    assert(Percentage.from(-0.1).isLeft)
    assert(Percentage.from(100.1).isLeft)
  }

  test("direct constructor or apply call does not compile") {
    assert(compileErrors("Percentage(50.0)").nonEmpty)
  }
