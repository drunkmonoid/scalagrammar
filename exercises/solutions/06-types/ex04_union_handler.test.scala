//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/06-types/05_union_intersection.scala

package solutions.types.unionhandler

def render(x: Int | String | Boolean): String = x match
  case i: Int     => s"int: $i"
  case s: String  => s"string: $s"
  case b: Boolean => s"bool: $b"

class UnionHandlerSuite extends munit.FunSuite:
  test("renders integers") {
    assertEquals(render(42), "int: 42")
    assertEquals(render(-1), "int: -1")
  }

  test("renders strings") {
    assertEquals(render("hello"), "string: hello")
    assertEquals(render(""), "string: ")
  }

  test("renders booleans") { // ⚠
    assertEquals(render(true), "bool: true")
    assertEquals(render(false), "bool: false")
  }
