//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/04-pattern-matching/01_patterns.scala

package solutions.patternmatching.expreval

enum Expr:
  case Num(value: Int)
  case Add(left: Expr, right: Expr)
  case Mul(left: Expr, right: Expr)

def eval(e: Expr): Int = e match
  case Expr.Num(v)    => v
  case Expr.Add(l, r) => eval(l) + eval(r)
  case Expr.Mul(l, r) => eval(l) * eval(r)

def simplify(e: Expr): Expr =
  val simplified = e match
    case Expr.Num(v)    => Expr.Num(v)
    case Expr.Add(l, r) =>
      (simplify(l), simplify(r)) match
        case (Expr.Num(0), r2) => r2
        case (l2, Expr.Num(0)) => l2
        case (l2, r2)          => Expr.Add(l2, r2)
    case Expr.Mul(l, r) =>
      (simplify(l), simplify(r)) match
        case (Expr.Num(0), _)  => Expr.Num(0)
        case (_, Expr.Num(0))  => Expr.Num(0)
        case (Expr.Num(1), r2) => r2
        case (l2, Expr.Num(1)) => l2
        case (l2, r2)          => Expr.Mul(l2, r2)

  if simplified == e then simplified else simplify(simplified)

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
