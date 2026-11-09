//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/02-functions/06_partial_functions.scala

package solutions.functions.collectlengths

val lengths: PartialFunction[Any, Int] =
  case s: String if s.nonEmpty    => s.length
  case xs: List[?] if xs.nonEmpty => xs.size

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
