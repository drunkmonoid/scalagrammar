//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/04-pattern-matching/01_patterns.scala

package exercises.patternmatching.describelist

// Task:
// Implement `describe(xs: List[Int]): String` using sequence pattern matching:
// - Nil / empty list: "empty"
// - Single element List(x): s"singleton: $x"
// - Starts with 0 (0 :: rest): "starts with zero"
// - Any other list: s"first: ${first}, last: ${last}"
// Example: describe(Nil) -> "empty", describe(List(42)) -> "singleton: 42", describe(List(0, 5)) -> "starts with zero", describe(List(1, 2, 3)) -> "first: 1, last: 3"

def describe(xs: List[Int]): String = ???

class DescribeListSuite extends munit.FunSuite:
  test("describes empty and singleton lists") {
    assertEquals(describe(Nil), "empty")
    assertEquals(describe(List(42)), "singleton: 42")
    assertEquals(describe(List(-5)), "singleton: -5")
  }

  test("describes lists starting with zero") { // ⚠
    assertEquals(describe(List(0)), "singleton: 0")
    assertEquals(describe(List(0, 1, 2)), "starts with zero")
  }

  test("describes longer lists with first and last elements") {
    assertEquals(describe(List(1, 2, 3)), "first: 1, last: 3")
    assertEquals(describe(List(10, 99)), "first: 10, last: 99")
  }
