//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/02-functions/06_partial_functions.scala

package exercises.functions.collectlengths

// Task:
// Build a `PartialFunction[Any, Int]` named `lengths` that is defined ONLY for:
// 1. Non-empty `String`s (returning the string length)
// 2. Non-empty `List[?]`s (returning the list size)
// For all other inputs (empty strings, empty lists, numbers, booleans, other collections),
// it must NOT be defined (`isDefinedAt` is false).
// Use this partial function with `collect` over heterogeneous lists.
// Example: List("hello", "", List(1, 2), Nil, 42).collect(lengths) -> List(5, 2)

val lengths: PartialFunction[Any, Int] = ???

class CollectLengthsSuite extends munit.FunSuite:
  test("collects lengths of non-empty strings and lists") {
    val input: List[Any] = List("scala", "", List(1, 2, 3), Nil, 42, true)
    assertEquals(input.collect(lengths), List(5, 3))
  }

  test("isDefinedAt correctly identifies matching and non-matching inputs") {
    assert(lengths.isDefinedAt("a"))
    assert(lengths.isDefinedAt(List(1)))
    assert(!lengths.isDefinedAt(""))
    assert(!lengths.isDefinedAt(Nil))
    assert(!lengths.isDefinedAt(123))
  }

  test("empty or unsupported structures are filtered out") { // ⚠
    val input: List[Any] = List("", Nil, Vector(1, 2, 3), Map("a" -> 1))
    assertEquals(input.collect(lengths), Nil)
  }
