//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/12-scala2/01_scala2_idioms.scala

package solutions.scala2.implicitconversion

import scala.language.implicitConversions

// Task:
// Migrate the following Scala 2.13 implicit conversion method to Scala 3 `given Conversion`:
//
// /* Scala 2.13:
// import scala.language.implicitConversions
// case class Seconds(value: Long)
// implicit def intToSeconds(n: Int): Seconds = Seconds(n.toLong)
// */
//
// Define `given Conversion[Int, Seconds]`.
// Example:
//   val s: Seconds = 10
//   s == Seconds(10L)

case class Seconds(value: Long)

given Conversion[Int, Seconds] with
  def apply(n: Int): Seconds = Seconds(n.toLong)

def waitDuration(s: Seconds): Long = s.value

class ImplicitConversionSuite extends munit.FunSuite:
  test("implicitly converts positive integer literals to Seconds") {
    val s: Seconds = 10
    assertEquals(s, Seconds(10L))
    assertEquals(waitDuration(5), 5L)
  }

  test("explicit conversion via Conversion given also works") {
    val conv = summon[Conversion[Int, Seconds]]
    assertEquals(conv(60), Seconds(60L))
  }

  test("handles zero and negative integers") { // ⚠
    val zeroSec: Seconds = 0
    assertEquals(zeroSec, Seconds(0L))
    val negSec: Seconds = -30
    assertEquals(negSec, Seconds(-30L))
  }
