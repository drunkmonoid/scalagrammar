//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/01-basics/05_control_flow.scala

package solutions.basics.firstduplicate

import scala.util.boundary
import scala.util.boundary.break

def firstDuplicate(xs: List[Int]): Option[Int] =
  boundary:
    var seen = Set.empty[Int]
    for x <- xs do
      if seen.contains(x) then break(Some(x))
      seen += x
    None

class FirstDuplicateSuite extends munit.FunSuite:
  test("finds adjacent duplicate") {
    assertEquals(firstDuplicate(List(1, 2, 2, 3)), Some(2))
  }

  test("returns None when all elements are distinct") {
    assertEquals(firstDuplicate(List(1, 2, 3, 4, 5)), None)
    assertEquals(firstDuplicate(Nil), None)
  }

  test("finds non-adjacent first duplicate") { // ⚠
    assertEquals(firstDuplicate(List(5, 1, 2, 3, 4, 1, 5)), Some(1))
    assertEquals(firstDuplicate(List(10, 20, 30, 20, 10)), Some(20))
  }
