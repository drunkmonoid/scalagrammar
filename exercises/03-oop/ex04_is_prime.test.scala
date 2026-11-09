//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/03-oop/08_extension_methods.scala

package exercises.oop.isprime

// Task:
// Implement an extension method `isPrime` on `Int` (`def isPrime: Boolean`).
// Numbers <= 1 (including 0, 1, and negative numbers) are NOT prime.
// A prime number > 1 is only divisible by 1 and itself.
// Example: 2.isPrime -> true, 4.isPrime -> false, 0.isPrime -> false, (-7).isPrime -> false

extension (n: Int)
  def isPrime: Boolean = ???

class IsPrimeSuite extends munit.FunSuite:
  test("identifies prime numbers") {
    assert(2.isPrime)
    assert(3.isPrime)
    assert(5.isPrime)
    assert(17.isPrime)
    assert(97.isPrime)
  }

  test("identifies composite numbers") {
    assert(!4.isPrime)
    assert(!9.isPrime)
    assert(!15.isPrime)
    assert(!100.isPrime)
  }

  test("non-positive numbers and 1 are not prime") { // ⚠
    assert(!0.isPrime)
    assert(!1.isPrime)
    assert(!(-1).isPrime)
    assert(!(-7).isPrime)
    assert(!Int.MinValue.isPrime)
  }
