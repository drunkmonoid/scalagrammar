//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/10-metaprogramming/01_inline.scala

package exercises.metaprogramming.inlinepower

// Task:
// Implement `inline def power(x: Double, inline n: Int): Double`.
// Using `inline if`, compute `x^n` at compile/inline time:
// - n == 0 -> 1.0
// - n > 0 -> x * power(x, n - 1)
// - n < 0 -> 1.0 / power(x, -n)
// A `compileErrors` test asserts that calling `power(2.0, nonConst)` does not compile.

inline def power(x: Double, inline n: Int): Double = ???

class InlinePowerSuite extends munit.FunSuite:
  test("computes power for positive and zero exponents") {
    assertEquals(power(2.0, 3), 8.0)
    assertEquals(power(5.0, 0), 1.0)
    assertEquals(power(3.0, 2), 9.0)
  }

  test("computes power for negative exponents") {
    assertEquals(power(2.0, -1), 0.5)
    assertEquals(power(2.0, -2), 0.25)
  }

  test("rejects non-constant exponents at compile time") { // ⚠
    assert(compileErrors("var n = 3; power(2.0, n)").nonEmpty)
  }
