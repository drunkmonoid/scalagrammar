//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/01-basics/04_strings_interpolation.scala

package exercises.basics.interpolator

// Task:
// Implement a string interpolator `title"..."` that capitalizes every interpolated argument
// (turning the first character to upper case) while leaving the literal string parts exactly as written.
// Example: val name = "alice"; title"hello $name" -> "hello Alice"

extension (sc: StringContext)
  def title(args: Any*): String = ???

class InterpolatorSuite extends munit.FunSuite:
  test("interpolates and capitalizes single argument") {
    val name = "alice"
    assertEquals(title"hello $name!", "hello Alice!")
  }

  test("interpolates multiple arguments leaving literal parts untouched") {
    val first = "bob"
    val city = "paris"
    assertEquals(title"$first visits $city", "Bob visits Paris")
  }

  test("handles empty string or non-letter arguments") { // ⚠
    val empty = ""
    val num = 42
    assertEquals(title"item: $empty, id: $num", "item: , id: 42")
  }
