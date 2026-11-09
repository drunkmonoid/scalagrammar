//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/05-collections/05_views_iterators_lazy.scala

package solutions.collections.primes

val primes: LazyList[Int] =
  def isPrime(n: Int): Boolean =
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

  LazyList.from(2).filter(isPrime)

class PrimesSuite extends munit.FunSuite:
  test("produces initial sequence of primes") {
    assertEquals(primes.take(5).toList, List(2, 3, 5, 7, 11))
  }

  test("generates the first 100 primes accurately") {
    val hundred = primes.take(100).toList
    assertEquals(hundred.length, 100)
    assertEquals(hundred.head, 2)
    assertEquals(hundred.last, 541)
  }

  test("primes are strictly monotonically increasing") { // ⚠
    val first20 = primes.take(20).toList
    assert(first20.zip(first20.tail).forall { case (a, b) => a < b })
  }
