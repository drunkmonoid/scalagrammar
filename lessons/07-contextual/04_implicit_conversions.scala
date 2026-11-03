//> using scala 3.9.0
// Run: scala-cli run lessons/07-contextual/04_implicit_conversions.scala
//
// Implicit conversions: given instances of scala.Conversion[A, B].
// They are powerful and dangerous; Scala 3 requires a language import at the DEFINITION
// site (feature warning otherwise) and they never chain.

package contextual.conversions

import scala.language.implicitConversions
import scala.compiletime.testing.typeCheckErrors

case class Meters(value: Double)
case class Feet(value: Double)

object Conversions:
  given Conversion[Feet, Meters] = f => Meters(f.value * 0.3048)
  given Conversion[Int, Meters] = i => Meters(i)

def distance(m: Meters): String = f"${m.value}%.2f m"

// Most conversions are better written as overloads, extension methods or type classes.
// A common legitimate use: "magnet"-style APIs that accept several argument types.
enum Arg:
  case IntArg(i: Int)
  case StrArg(s: String)
object Arg:
  given Conversion[Int, Arg] = IntArg(_)
  given Conversion[String, Arg] = StrArg(_)
def command(args: Arg*): String = args.map {
  case Arg.IntArg(i) => s"int:$i"
  case Arg.StrArg(s) => s"str:$s"
}.mkString(" ")

@main def run(): Unit =
  section("Conversions apply when types don't match")
  import Conversions.given
  println(distance(Feet(10)))      // Feet → Meters inserted by the compiler
  println(distance(5))             // Int → Meters
  // They also apply to member selection: `x.foo` where x lacks foo but the target has it
  println(Feet(1).value)           // Feet HAS value, so no conversion

  section("Conversions don't chain")
  case class Yards(v: Double)
  given Conversion[Yards, Feet] = y => Feet(y.v * 3)
  doesNotCompile("contextual.conversions.distance(Yards(1))")   // Yards→Feet→Meters: NO
  println(distance(summon[Conversion[Yards, Feet]](Yards(1))))   // apply one explicitly

  section("Conversions in the target's companion are found automatically")
  println(command(1, "two", 3))    // Arg companion has the conversions

  section("Standard library conversions you use daily")
  // Predef: Int → RichInt (1 to 10, 5.max(3)), String → StringOps, Array → ArraySeq/ArrayOps,
  // int2Integer / Integer2int for Java boxing, Int → Long/Double numeric widening.
  val n: java.lang.Integer = 5     // int2Integer
  val m: Int = n                   // Integer2int — ⚠ NPE if n is null!
  println(n -> m)
  val nullBox: java.lang.Integer = null
  try { val boom: Int = nullBox; println(boom) }
  catch case e: NullPointerException => println("  Integer2int(null) → NullPointerException")

  section("Dangers")
  // A conversion from a common type (String, Int) changes the meaning of unrelated code:
  // given Conversion[String, Int] = _.length  → "abc" * 2 might now be ambiguous or wrong.
  // Prefer explicit methods (.toMeters), extension methods, or type classes.
  println("see comments")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
