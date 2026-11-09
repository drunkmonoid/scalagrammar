//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/02-functions/05_recursion_tailrec.scala

package exercises.functions.digits

import scala.annotation.tailrec

// Task:
// Implement a tail-recursive function `sumDigits(n: Long): Int` annotated with `@scala.annotation.tailrec`.
// It computes the sum of the decimal digits of `n`.
// Negative numbers should sum the digits of their absolute value (e.g. -123 -> 1 + 2 + 3 = 6).
// Be careful with Long.MinValue, since -Long.MinValue overflows Long!
// Example: sumDigits(12345L) -> 15, sumDigits(-42L) -> 6

def sumDigits(n: Long): Int = ???

class DigitsSuite extends munit.FunSuite:
  test("computes sum of positive digits") {
    assertEquals(sumDigits(12345L), 15)
    assertEquals(sumDigits(0L), 0)
    assertEquals(sumDigits(999L), 27)
  }

  test("computes sum of negative digits") {
    assertEquals(sumDigits(-42L), 6)
    assertEquals(sumDigits(-100L), 1)
  }

  test("handles Long.MinValue without overflow") { // ⚠
    assertEquals(sumDigits(Long.MinValue), 89)
    assertEquals(sumDigits(Long.MaxValue), 88)
  }
