//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/06-types/02_generics_variance.scala

package exercises.types.boxvariance

// Task:
// The stub declares an invariant `final case class Box[A](value: A)` with `def replace[B >: A](b: B): Box[B] = ???`.
// Make `Box` covariant (`Box[+A]`) and implement `replace[B >: A](b: B): Box[B]`.
// A "make this compile" test checks that Box is covariant and compiles when assigning Box[Int] to Box[Any].
// Example: val b: Box[Any] = Box(42); b.replace("hello") -> Box("hello")

final case class Box[A](value: A):
  def replace[B >: A](b: B): Box[B] = ???

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
