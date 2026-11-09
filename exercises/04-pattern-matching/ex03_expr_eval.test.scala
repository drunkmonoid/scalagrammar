//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/04-pattern-matching/01_patterns.scala

package exercises.patternmatching.expreval

// Task:
// Define an algebraic data type `enum Expr` with:
//   - `Num(value: Int)`
//   - `Add(left: Expr, right: Expr)`
//   - `Mul(left: Expr, right: Expr)`
// Implement:
//   1. `def eval(e: Expr): Int`: evaluates the expression to an integer.
//   2. `def simplify(e: Expr): Expr`: simplifies expressions using algebraic identities:
//      - x * 1 -> x, 1 * x -> x
//      - x + 0 -> x, 0 + x -> x
//      - 0 * x -> 0 (Num(0)), x * 0 -> 0 (Num(0))
//      ⚠ Nested expressions must be simplified recursively until no more rules apply.

enum Expr:
  case Num(value: Int)
  case Add(left: Expr, right: Expr)
  case Mul(left: Expr, right: Expr)

def eval(e: Expr): Int = ???
def simplify(e: Expr): Expr = ???

class ExprEvalSuite extends munit.FunSuite:
  import Expr.*

  test("eval computes arithmetic result correctly") {
    val expr = Add(Num(10), Mul(Num(2), Num(3)))
    assertEquals(eval(expr), 16)
  }

  test("simplify applies basic identity rules") {
    assertEquals(simplify(Mul(Num(42), Num(1))), Num(42))
    assertEquals(simplify(Mul(Num(1), Num(42))), Num(42))
    assertEquals(simplify(Add(Num(42), Num(0))), Num(42))
    assertEquals(simplify(Add(Num(0), Num(42))), Num(42))
    assertEquals(simplify(Mul(Num(42), Num(0))), Num(0))
    assertEquals(simplify(Mul(Num(0), Num(42))), Num(0))
  }

  test("simplify handles nested expressions recursively") { // ⚠
    val nested1 = Mul(Add(Num(5), Num(0)), Num(1))
    assertEquals(simplify(nested1), Num(5))

    val nested2 = Add(Mul(Num(99), Num(0)), Mul(Num(7), Num(1)))
    assertEquals(simplify(nested2), Num(7))
  }
