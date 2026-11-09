//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/05-collections/05_views_iterators_lazy.scala

package exercises.collections.windowmax

// Task:
// Implement `windowMax(xs: List[Int], k: Int): List[Int]`.
// It slides a window of size `k` across `xs` (step 1) and computes the maximum element in each window.
// ⚠ If k <= 0 or k > xs.length or xs is empty, return Nil.
// Example: windowMax(List(1, 3, -1, -3, 5, 3, 6, 7), 3) -> List(3, 3, 5, 5, 6, 7)

def windowMax(xs: List[Int], k: Int): List[Int] = ???

class WindowMaxSuite extends munit.FunSuite:
  test("computes maximum over sliding window") {
    val input = List(1, 3, -1, -3, 5, 3, 6, 7)
    assertEquals(windowMax(input, 3), List(3, 3, 5, 5, 6, 7))
  }

  test("returns single max when k equals list size") {
    val input = List(10, 5, 20, 15)
    assertEquals(windowMax(input, 4), List(20))
  }

  test("returns Nil when k is larger than list or non-positive") { // ⚠
    val input = List(1, 2, 3)
    assertEquals(windowMax(input, 5), Nil)
    assertEquals(windowMax(input, 0), Nil)
    assertEquals(windowMax(input, -2), Nil)
    assertEquals(windowMax(Nil, 2), Nil)
  }
