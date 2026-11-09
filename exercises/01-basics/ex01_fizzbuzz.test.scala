//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/01-basics/05_control_flow.scala

package exercises.basics.fizzbuzz

// Task:
// Implement `fizz(n: Int): String` using pattern matching with guards (`match`).
// Multiples of 3 should return "Fizz".
// Multiples of 5 should return "Buzz".
// Multiples of both 3 and 5 should return "FizzBuzz".
// Any other number should return the number as a string (e.g. "7").
// Example: fizz(15) -> "FizzBuzz", fizz(3) -> "Fizz"

def fizz(n: Int): String = ???

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
