//> using scala 3.9.0
// Run: scala-cli run lessons/06-types/09_singleton_literal_types.scala
//
// Singleton types (x.type), literal types (42, "a", true), ValueOf, Singleton bound,
// and widening rules.

package types.singletons

import scala.compiletime.testing.typeCheckErrors
import scala.compiletime.constValue

object Config:
  val name = "app"

object Lits:
  val a = 1                 // Int
  final val b = 1           // type 1 — a constant, folded into use sites
  inline val c = 1          // type 1 — guaranteed inlined (must be a constant)

// A method that only accepts the literal 0 or 1
def bit(b: 0 | 1): String = s"bit $b"

// Using literal types as type-level tags
final class Vec[N <: Int](val elems: Vector[Double]):
  def +(o: Vec[N]): Vec[N] = new Vec[N](elems.lazyZip(o.elems).map(_ + _))
  override def toString = elems.mkString("Vec(", ", ", ")")
object Vec:
  inline def apply[N <: Int](xs: Double*): Vec[N] =
    require(xs.size == constValue[N], s"expected ${constValue[N]} elements")
    new Vec[N](xs.toVector)

// ValueOf: get the unique value of a singleton type as an implicit
def valueOfType[T](using v: ValueOf[T]): T = v.value

// The Singleton bound: forces inference of a precise singleton type
def precise[T <: Singleton](t: T): T = t

@main def run(): Unit =
  section("Literal types")
  val one: 1 = 1
  val hi: "hi" = "hi"
  val t: true = true
  println(s"$one $hi $t")
  doesNotCompile("val two: 2 = 3")
  println(bit(1))
  doesNotCompile("types.singletons.bit(2)")

  section("Widening of inferred types")
  val a = 1                 // inferred Int (widened)
  summon[Lits.b.type <:< 1] // `final val` keeps the literal type
  summon[Lits.c.type <:< 1]
  println(s"$a ${Lits.b} ${Lits.c}")
  var v: 1 | 2 = 1
  v = 2
  doesNotCompile("var v: 1 | 2 = 1; v = 3")

  section("Singleton types of objects and vals")
  val n: Config.type = Config
  println(n.name)
  val s = "x"
  val sameS: s.type = s           // only s itself inhabits s.type
  println(sameS)
  doesNotCompile("val s = \"x\"; val other: s.type = \"x\"")   // even an equal value doesn't fit

  section("Literal-typed sizes catch errors at compile time")
  val v3a = Vec[3](1, 2, 3)
  val v3b = Vec[3](10, 20, 30)
  println(v3a + v3b)
  val v2 = Vec[2](1, 2)
  doesNotCompile("types.singletons.Vec[3](1, 2, 3) + types.singletons.Vec[2](1, 2)")
  try Vec[2](1, 2, 3) catch case e: IllegalArgumentException => println(s"  ${e.getMessage}")
  println(v2)

  section("ValueOf")
  println(valueOfType[42])
  println(valueOfType["literal"])
  println(valueOfType[Config.type].name)
  println(valueOf[7])                     // Predef helper

  section("Singleton bound")
  // Inside `precise`, T is inferred as the singleton type 42 rather than Int.
  // ⚠ But inferred types of vals (and of type arguments of enclosing calls like List(...))
  //   are still widened to Int, so annotate when you want to keep the literal type:
  val p = precise(42)                     // p: Int
  val q: 42 = precise(42)                 // q: 42
  println(p -> q)

  section("Arithmetic on literal types (see 07 for more)")
  import scala.compiletime.ops.int.+
  val sum: 2 + 3 = 5
  println(sum)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
