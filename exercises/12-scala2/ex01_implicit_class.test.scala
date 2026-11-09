//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/12-scala2/01_scala2_idioms.scala

package exercises.scala2.implicitclass

// Task:
// Migrate the following Scala 2.13 implicit class syntax to Scala 3 `extension`:
//
// /* Scala 2.13:
// implicit class RichInt(private val n: Int) extends AnyVal {
//   def isEven: Boolean = n % 2 == 0
//   def times[A](f: => A): List[A] = (1 to n).map(_ => f).toList
// }
// */
//
// Implement `isEven` and `times` as Scala 3 extension methods on `Int`.
// Example:
//   4.isEven == true
//   3.times("hi") == List("hi", "hi", "hi")

extension (n: Int)
  def isEven: Boolean = ???
  def times[A](f: => A): List[A] = ???

class ImplicitClassSuite extends munit.FunSuite:
  test("isEven returns expected parity") {
    assertEquals(2.isEven, true)
    assertEquals(3.isEven, false)
    assertEquals(0.isEven, true)
  }

  test("times evaluates block n times into a list") {
    var count = 0
    val res = 3.times {
      count += 1
      count
    }
    assertEquals(res, List(1, 2, 3))
    assertEquals(count, 3)
  }

  test("handles zero and negative counts") { // ⚠
    assertEquals((-2).isEven, true)
    assertEquals((-3).isEven, false)
    assertEquals(0.times("x"), Nil)
    assertEquals((-5).times("y"), Nil)
  }
