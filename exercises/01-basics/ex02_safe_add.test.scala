//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/01-basics/03_types_literals.scala

package exercises.basics.safeadd

// Task:
// Implement `safeAdd(a: Int, b: Int): Option[Int]` without using Long arithmetic.
// Return Some(sum) when addition does not overflow or underflow.
// Return None when addition overflows (> Int.MaxValue) or underflows (< Int.MinValue).
// Example: safeAdd(10, 20) -> Some(30), safeAdd(Int.MaxValue, 1) -> None

def safeAdd(a: Int, b: Int): Option[Int] = ???

class SafeAddSuite extends munit.FunSuite:
  test("adds within normal range") {
    assertEquals(safeAdd(10, 20), Some(30))
    assertEquals(safeAdd(-10, -20), Some(-30))
    assertEquals(safeAdd(100, -50), Some(50))
  }

  test("overflow returns None") { // ⚠
    assertEquals(safeAdd(Int.MaxValue, 1), None)
    assertEquals(safeAdd(Int.MaxValue - 10, 20), None)
  }

  test("underflow returns None") { // ⚠
    assertEquals(safeAdd(Int.MinValue, -1), None)
    assertEquals(safeAdd(Int.MinValue + 5, -10), None)
  }
