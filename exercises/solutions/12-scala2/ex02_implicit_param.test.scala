//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/12-scala2/01_scala2_idioms.scala

package solutions.scala2.implicitparam

// Task:
// Migrate the following Scala 2.13 implicit parameters and values to Scala 3 `given` and `using`:
//
// /* Scala 2.13:
// trait Formatter[A] {
//   def format(value: A): String
// }
// object Formatter {
//   implicit val intFormatter: Formatter[Int] = (n: Int) => f"$n%,d"
//   implicit val booleanFormatter: Formatter[Boolean] = (b: Boolean) => if (b) "YES" else "NO"
// }
// def render[A](value: A)(implicit f: Formatter[A]): String = f.format(value)
// */
//
// Define `given Formatter[Int]`, `given Formatter[Boolean]`, and `render[A]` using `using`.
// Example:
//   render(1000) == "1,000"
//   render(true) == "YES"

trait Formatter[A]:
  def format(value: A): String

given Formatter[Int] with
  def format(value: Int): String = f"$value%,d"

given Formatter[Boolean] with
  def format(value: Boolean): String = if value then "YES" else "NO"

def render[A](value: A)(using f: Formatter[A]): String = f.format(value)

class ImplicitParamSuite extends munit.FunSuite:
  test("renders formatted integers") {
    assertEquals(render(1000), "1,000")
    assertEquals(render(42), "42")
  }

  test("renders formatted booleans") {
    assertEquals(render(true), "YES")
    assertEquals(render(false), "NO")
  }

  test("handles negative integers and large numbers") { // ⚠
    assertEquals(render(-1000000), "-1,000,000")
    assertEquals(render(0), "0")
  }
