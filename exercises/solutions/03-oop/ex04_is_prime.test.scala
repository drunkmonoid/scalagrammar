//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/03-oop/08_extension_methods.scala

package solutions.oop.isprime

extension (n: Int)
  def isPrime: Boolean =
    if n <= 1 then false
    else if n <= 3 then true
    else if n % 2 == 0 || n % 3 == 0 then false
    else
      var d = 5
      var prime = true
      while d * d <= n && prime do
        if n % d == 0 || n % (d + 2) == 0 then prime = false
        d += 6
      prime

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
