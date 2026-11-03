//> using scala 3.9.0
//> using test.dep org.scalameta::munit-scalacheck:1.0.0
// Run: scala-cli test ecosystem/testing/scalacheck.test.scala
//
// Property-based testing with ScalaCheck: instead of hand-picked examples, state a PROPERTY
// that must hold for all inputs; ScalaCheck generates hundreds of random inputs and, on
// failure, SHRINKS the counter-example to a minimal one.
// Here via munit-scalacheck (ScalaCheckSuite); ScalaTest has ScalaCheckPropertyChecks.

package ecosystem.testing.propertydemo

import org.scalacheck.{Arbitrary, Gen, Prop}
import org.scalacheck.Prop.forAll

// ---------- code under test ----------
object Codec:
  // Run-length encoding: "aaab" → List(('a',3), ('b',1))
  def encode(s: String): List[(Char, Int)] =
    s.foldRight(List.empty[(Char, Int)]) {
      case (c, (d, n) :: rest) if c == d => (d, n + 1) :: rest
      case (c, acc)                      => (c, 1) :: acc
    }
  def decode(xs: List[(Char, Int)]): String = xs.map((c, n) => c.toString * n).mkString

case class Point(x: Int, y: Int)

// ---------- custom generators ----------
object Generators:
  val smallPositive: Gen[Int] = Gen.choose(1, 100)
  val point: Gen[Point] = for
    x <- Gen.choose(-1000, 1000)
    y <- Gen.choose(-1000, 1000)
  yield Point(x, y)
  given Arbitrary[Point] = Arbitrary(point)
  val email: Gen[String] = for
    user <- Gen.alphaLowerStr.suchThat(_.nonEmpty)
    host <- Gen.oneOf("example.org", "scala-lang.org")
  yield s"$user@$host"

class PropertiesSuite extends munit.ScalaCheckSuite:
  import Generators.given

  property("decode is the inverse of encode (round trip)") {
    forAll { (s: String) => Codec.decode(Codec.encode(s)) == s }
  }

  property("encoded runs never have adjacent equal characters") {
    forAll { (s: String) =>
      val runs = Codec.encode(s)
      runs.zip(runs.drop(1)).forall((a, b) => a._1 != b._1)
    }
  }

  property("reverse is an involution") {
    forAll { (xs: List[Int]) => xs.reverse.reverse == xs }
  }

  property("sorting is idempotent and preserves size") {
    forAll { (xs: List[Int]) =>
      val s = xs.sorted
      s.sorted == s && s.size == xs.size
    }
  }

  property("custom generator: points stay in range") {
    forAll { (p: Point) => math.abs(p.x) <= 1000 && math.abs(p.y) <= 1000 }
  }

  property("explicit generator") {
    forAll(Generators.email) { e => e.count(_ == '@') == 1 }
  }

  property("conditional properties with ==>") {
    import Prop.propBoolean
    forAll { (n: Int) => (n > 0) ==> (n.toLong * 2 > n) }
  }

  property("monoid laws for string concatenation") {
    forAll { (a: String, b: String, c: String) =>
      ((a + b) + c == a + (b + c)) && ("" + a == a) && (a + "" == a)
    }
  }

  // A property that FAILS would print the shrunk counter-example, e.g. for
  //   forAll { (xs: List[Int]) => xs.sum < 100 }
  // → Falsified after N passed tests. ARG_0: List(100)   (shrunk from a long random list)
  test("show a shrunk counter-example without failing the suite") {
    val result = org.scalacheck.Test.check(org.scalacheck.Test.Parameters.default,
      forAll((xs: List[Int]) => xs.sum < 100))
    println(s"  ScalaCheck result: ${result.status}")
    assert(!result.passed)
  }
