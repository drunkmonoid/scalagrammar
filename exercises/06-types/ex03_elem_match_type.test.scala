//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/06-types/07_type_lambdas_match_types.scala

package exercises.types.elemmatchtype

// Task:
// Define a match type `Elem[X]` that extracts the element type of `X`:
//   - String -> Char
//   - List[t] -> t
//   - Option[t] -> t
// Implement `def first[X](x: X): Elem[X]` returning:
//   - For String: x.head
//   - For List[t]: x.head
//   - For Option[t]: x.get
// Example: first("scala") -> 's', first(List(1, 2, 3)) -> 1, first(Some("ok")) -> "ok"

type Elem[X] = X match
  case String    => Char
  case List[t]   => t
  case Option[t] => t

def first[X](x: X): Elem[X] = ???

class ElemMatchTypeSuite extends munit.FunSuite:
  test("extracts first character from String") {
    val ch: Char = first("hello")
    assertEquals(ch, 'h')
  }

  test("extracts first element from List") {
    val n: Int = first(List(10, 20, 30))
    assertEquals(n, 10)
    val s: String = first(List("alpha", "beta"))
    assertEquals(s, "alpha")
  }

  test("extracts element from Option") { // ⚠
    val opt: Option[Double] = Some(3.14)
    val d: Double = first(opt)
    assertEquals(d, 3.14)
  }
