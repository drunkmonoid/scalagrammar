//> using scala 3.9.0
// Run: scala-cli run lessons/10-metaprogramming/01_inline.scala
//
// `inline` is Scala 3's first metaprogramming tool: GUARANTEED inlining at compile time,
// plus compile-time evaluation of conditions and matches. No macros needed.

package meta.inlining

import scala.compiletime.{error, constValue, codeOf}
import scala.compiletime.testing.typeCheckErrors

// ---------- inline methods ----------
inline def debugLog(inline msg: String): Unit =
  inline if Config.debug then println(s"  [debug] $msg")   // whole call disappears when debug=false

object Config:
  inline val debug = true                  // inline val: a compile-time constant

// ---------- inline parameters: the argument expression is substituted, not evaluated first ----------
inline def twice(inline x: Int): Int = x + x
def twiceByValue(x: Int): Int = x + x

// ---------- recursive inline with constant folding ----------
inline def power(x: Double, inline n: Int): Double =
  inline if n == 0 then 1.0
  else inline if n % 2 == 1 then x * power(x, n - 1)
  else
    val y = power(x, n / 2)
    y * y
// power(x, 5) expands at compile time to straight-line multiplications.

// ---------- inline match: chosen at compile time from the STATIC type ----------
transparent inline def describe(x: Any): Any = inline x match
  case _: Int    => "an Int"
  case _: String => 42                     // transparent: result type is the precise branch type
  case _         => false

// ---------- transparent inline: precise result types ----------
transparent inline def zeroOf(inline kind: String): Any = inline kind match
  case "int"    => 0
  case "string" => ""
  case "list"   => List.empty[Int]

// ---------- compile-time errors with custom messages ----------
inline def checkedPercentage(inline p: Int): Int =
  inline if p < 0 || p > 100 then error("percentage must be between 0 and 100: " + codeOf(p))
  else p

// ---------- inline + by-name: zero-cost assertions ----------
inline def assertPositive(inline x: Int): Unit =
  if x <= 0 then throw IllegalArgumentException(s"${codeOf(x)} must be positive, was $x")

// ---------- inline methods can't be overridden unless... ----------
trait Logger:
  inline def info(msg: String): Unit = println(s"  [info] $msg")   // effectively final
abstract class Base:
  def compute(x: Int): Int
class Impl extends Base:
  inline def compute(x: Int): Int = x * 10    // inline override of an abstract method is OK

@main def run(): Unit =
  section("Inline conditionals")
  debugLog("visible because Config.debug is true")

  section("inline parameters duplicate the expression")
  def noisy(): Int = { println("  evaluating noisy()"); 5 }
  println(twiceByValue(noisy()))           // evaluated once
  println(twice(noisy()))                  // ⚠ evaluated TWICE: x + x became noisy() + noisy()

  section("Compile-time unrolling")
  println(power(2.0, 10))                  // 1024.0, no loop/recursion at runtime

  section("inline match on static types")
  val a = describe(1)
  val b = describe("s")
  val c = describe(1.5)
  println(s"$a / $b / $c")
  val bInt: Int = b                        // b's static type is Int (42) thanks to `transparent`
  println(bInt + 1)
  val any: Any = "a string"
  println(describe(any))                   // ⚠ false — static type is Any, not String

  section("transparent inline result types")
  val i: Int = zeroOf("int")
  val s: String = zeroOf("string")
  val l: List[Int] = zeroOf("list")
  println((i, s, l))
  doesNotCompile("meta.inlining.zeroOf(\"other\")")   // no case matches → compile error

  section("Custom compile errors")
  println(checkedPercentage(42))
  doesNotCompile("meta.inlining.checkedPercentage(150)")

  section("codeOf: the source text of an inline argument")
  try assertPositive(3 - 5)
  catch case e: IllegalArgumentException => println(s"  ${e.getMessage}")

  section("inline overrides")
  println(Impl().compute(4))
  val asBase: Base = Impl()
  println(asBase.compute(4))               // dynamic dispatch still works (a forwarder is kept)

  section("Rules and limits")
  // * inline methods are expanded at EVERY call site (code size!)
  // * the max inline depth is limited (-Xmax-inlines, default 32)
  // * `inline if` / `inline match` REQUIRE a reducible condition, else compile error
  // * inline defs are effectively final; they can implement abstract methods
  // * inline params are not evaluated before the call (unlike by-value), but can be
  //   evaluated multiple times (unlike a cached by-name)
  doesNotCompile("def f(n: Int) = meta.inlining.power(2.0, n)")   // n not a constant → can't reduce

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
