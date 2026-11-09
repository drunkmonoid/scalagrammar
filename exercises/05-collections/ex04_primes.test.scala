//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/05-collections/05_views_iterators_lazy.scala

package exercises.collections.primes

// Task:
// Implement `primes: LazyList[Int]`, an infinite lazy sequence of prime numbers (2, 3, 5, 7, 11, ...).
// Use trial division or sieve over lazy streams.
// Tests verify the sequence by taking the first 100 primes.
// Example: primes.take(5).toList -> List(2, 3, 5, 7, 11)

val primes: LazyList[Int] = ???

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
