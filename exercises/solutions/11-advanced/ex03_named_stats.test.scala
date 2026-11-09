//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/11-advanced/03_named_tuples.scala

package solutions.advanced.namedstats

// Task:
// Implement `def stats(xs: List[Double]): Option[(min: Double, max: Double, mean: Double)]`.
// For a non-empty list of doubles, return `Some` containing a named tuple with the minimum,
// maximum, and arithmetic mean.
// For an empty list, return `None` without throwing an exception.
// Example:
//   stats(List(1.0, 3.0, 8.0)) == Some((min = 1.0, max = 8.0, mean = 4.0))

def stats(xs: List[Double]): Option[(min: Double, max: Double, mean: Double)] =
  if xs.isEmpty then None
  else
    val min = xs.min
    val max = xs.max
    val mean = xs.sum / xs.size
    Some((min = min, max = max, mean = mean))

class NamedStatsSuite extends munit.FunSuite:
  test("computes stats for non-empty list of doubles") {
    val res = stats(List(1.0, 3.0, 8.0))
    assertEquals(res, Some((min = 1.0, max = 8.0, mean = 4.0)))
    assertEquals(res.get.min, 1.0)
    assertEquals(res.get.max, 8.0)
    assertEquals(res.get.mean, 4.0)
  }

  test("computes stats for list with single element or negative numbers") {
    assertEquals(stats(List(5.0)), Some((min = 5.0, max = 5.0, mean = 5.0)))
    assertEquals(stats(List(-4.0, 0.0, 4.0)), Some((min = -4.0, max = 4.0, mean = 0.0)))
  }

  test("returns None for empty list without throwing") { // ⚠
    assertEquals(stats(Nil), None)
    assertEquals(stats(List.empty[Double]), None)
  }
