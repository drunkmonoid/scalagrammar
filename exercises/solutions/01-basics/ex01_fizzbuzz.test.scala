//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/01-basics/05_control_flow.scala

package solutions.basics.fizzbuzz

def fizz(n: Int): String = n match
  case _ if n % 3 == 0 && n % 5 == 0 => "FizzBuzz"
  case _ if n % 3 == 0               => "Fizz"
  case _ if n % 5 == 0               => "Buzz"
  case _                             => n.toString

class FizzBuzzSuite extends munit.FunSuite:
  test("standard multiples of 3 and 5") {
    assertEquals(fizz(3), "Fizz")
    assertEquals(fizz(5), "Buzz")
    assertEquals(fizz(15), "FizzBuzz")
    assertEquals(fizz(7), "7")
  }

  test("zero evaluates to FizzBuzz") { // ⚠
    assertEquals(fizz(0), "FizzBuzz")
  }

  test("negative multiples retain divisibility") { // ⚠
    assertEquals(fizz(-3), "Fizz")
    assertEquals(fizz(-5), "Buzz")
    assertEquals(fizz(-15), "FizzBuzz")
    assertEquals(fizz(-7), "-7")
  }
