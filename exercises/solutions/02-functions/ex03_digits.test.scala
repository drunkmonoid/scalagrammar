//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/02-functions/05_recursion_tailrec.scala

package solutions.functions.digits

import scala.annotation.tailrec

def sumDigits(n: Long): Int =
  @tailrec
  def loop(rem: Long, acc: Int): Int =
    if rem == 0L then acc
    else
      val digit = math.abs((rem % 10L).toInt)
      loop(rem / 10L, acc + digit)

  if n == 0L then 0
  else loop(n, 0)

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
