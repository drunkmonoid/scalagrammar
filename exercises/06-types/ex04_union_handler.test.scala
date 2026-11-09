//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/06-types/05_union_intersection.scala

package exercises.types.unionhandler

// Task:
// Implement `def render(x: Int | String | Boolean): String`.
// It must pattern match exhaustively on the three union types with NO wildcard/default case (`case _`):
//   - Int: s"int: $x"
//   - String: s"string: $x"
//   - Boolean: s"bool: $x"
// Example: render(42) -> "int: 42", render("hi") -> "string: hi", render(true) -> "bool: true"

def render(x: Int | String | Boolean): String = ???

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
