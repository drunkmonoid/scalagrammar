//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/01-basics/05_control_flow.scala

package exercises.basics.firstduplicate

// Task:
// Implement `firstDuplicate(xs: List[Int]): Option[Int]` using `scala.util.boundary`
// and `boundary.break` without using `return`.
// It should return the first element that appears a second time as you traverse from left to right.
// Return None if all elements are distinct.
// Example: firstDuplicate(List(1, 2, 3, 2, 1)) -> Some(2)

def firstDuplicate(xs: List[Int]): Option[Int] = ???

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
