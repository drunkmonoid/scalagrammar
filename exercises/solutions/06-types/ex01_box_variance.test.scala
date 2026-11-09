//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/06-types/02_generics_variance.scala

package solutions.types.boxvariance

final case class Box[+A](value: A):
  def replace[B >: A](b: B): Box[B] = Box(b)

class BoxVarianceSuite extends munit.FunSuite:
  test("replace updates the boxed value") {
    val b = Box(10)
    assertEquals(b.replace(20), Box(20))
  }

  test("replace widens the type") {
    val b: Box[Int] = Box(10)
    val b2: Box[Any] = b.replace("hello")
    assertEquals(b2.value, "hello")
  }

  test("Box is covariant allowing assignment to supertype") { // ⚠
    assertNoDiff(compileErrors("val bInt: Box[Int] = Box(42); val bAny: Box[Any] = bInt"), "")
  }
