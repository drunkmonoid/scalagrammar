//> using scala 3.9.0
// See also: lessons/07-contextual/06_resolution_rules.scala
// See also: lessons/10-metaprogramming/02_compiletime.scala
// Run: scala-cli run lessons/07-contextual/07_notgiven.scala
//
// scala.util.NotGiven: negative evidence in given resolution.
// For conditional given-summoning branching, see `summonFrom` in
// lessons/10-metaprogramming/02_compiletime.scala.

package contextual.notgiven

import scala.compiletime.testing.typeCheckErrors
import scala.util.NotGiven

trait Show[A]:
  def show(a: A): String

object Show:
  given Show[Int] = i => s"Int($i)"
  given Show[String] = s => s"String($s)"

case class Unformatted(data: String)

// Accepts A only when NO given Show[A] is in implicit scope
def requireNoShow[A](a: A)(using NotGiven[Show[A]]): String =
  s"Accepted without Show: $a"

@main def run(): Unit =
  section("Negative given evidence with scala.util.NotGiven")
  // Unformatted has no Show instance in scope, so NotGiven[Show[Unformatted]] is satisfied:
  val res = requireNoShow(Unformatted("raw-data"))
  println(res) // Accepted without Show: Unformatted(raw-data)

  section("Compile-time rejection when a given is present")
  // Show[Int] is in scope, so NotGiven[Show[Int]] cannot be synthesized:
  doesNotCompile("requireNoShow(42)")

  section("Ambiguous givens at identical priority")
  // Two instances of the same type in the same scope cause ambiguity:
  doesNotCompile("""
    trait AmbiguousTag
    given g1: AmbiguousTag = new AmbiguousTag {}
    given g2: AmbiguousTag = new AmbiguousTag {}
    summon[AmbiguousTag]
  """)

  section("Diverging implicit search")
  // A given whose own constraint directly requires itself produces a diverging search:
  doesNotCompile("""
    trait Loop[A]
    given loop[A](using Loop[A]): Loop[A] = new Loop[A] {}
    summon[Loop[Int]]
  """)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
