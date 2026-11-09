//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/10-metaprogramming/02_compiletime.scala

package solutions.metaprogramming.tuplesize

import scala.compiletime.erasedValue

// Task:
// Implement `inline def size[T <: Tuple]: Int`.
// Using `inline erasedValue[T] match`, recursively compute the number of elements
// in the tuple type `T` at compile time:
// - EmptyTuple -> 0
// - (_ *: t)   -> 1 + size[t]

inline def size[T <: Tuple]: Int =
  inline erasedValue[T] match
    case _: EmptyTuple => 0
    case _: (h *: t)   => 1 + size[t]

class TupleSizeSuite extends munit.FunSuite:
  test("computes size of empty tuple") {
    assertEquals(size[EmptyTuple], 0)
  }

  test("computes size of multi-element tuples") {
    assertEquals(size[(Int, String, Boolean)], 3)
    assertEquals(size[(Double, Double)], 2)
  }

  test("computes size of single-element and nested tuples") { // ⚠
    assertEquals(size[Tuple1[Int]], 1)
    assertEquals(size[(Int, (String, Double), Boolean, Char, Long)], 5)
  }
