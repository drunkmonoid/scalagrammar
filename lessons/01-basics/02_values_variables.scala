//> using scala 3.9.0
// Run: scala-cli run lessons/01-basics/02_values_variables.scala
//
// val, var, lazy val, def — and "everything is an expression".

package basics.values

import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("val: immutable binding (not immutable object!)")
  val xs = scala.collection.mutable.ArrayBuffer(1, 2)
  xs += 3                         // fine: the *object* is mutable
  println(xs)                     // ArrayBuffer(1, 2, 3)
  doesNotCompile("val a = 1; a = 2")

  section("var: reassignable")
  var counter = 0
  counter += 1                    // desugars to counter = counter + 1
  println(counter)                // 1

  section("Type inference and explicit types")
  val i = 42                      // Int
  val l = 42L                     // Long
  val d: Double = 42              // Int literal widened to Double
  val any: Any = 42               // upcast
  println(s"$i $l $d $any")
  // ⚠ inference picks the most specific type of the *initializer*:
  var s = "hi"                    // s: String
  doesNotCompile("""var s = "hi"; s = 1""")
  // ⚠ the if/else expression itself has type Int | String, but when a val's type is
  // *inferred*, Scala 3 widens such "soft" unions to their join (a common supertype,
  // here Matchable). Scala 2 inferred Any. Annotate to keep the union:
  val lub = if i > 0 then 1 else "one"   // lub: Matchable
  println(s"runtime class: ${lub.getClass.getSimpleName}")   // Integer (boxed!)
  val explicit: Int | String = if i > 0 then 1 else "one"
  println(explicit)

  section("lazy val: computed once, on first access, thread-safely")
  lazy val expensive = { println("  computing..."); 99 }
  println("before access")
  println(expensive)              // prints "computing..." then 99
  println(expensive)              // 99 (cached)
  // ⚠ if the initializer throws, the lazy val is NOT cached; next access retries.
  var attempts = 0
  lazy val flaky: Int = { attempts += 1; if attempts < 2 then throw RuntimeException("boom") else attempts }
  try flaky catch case e: RuntimeException => println(s"first access threw: ${e.getMessage}")
  println(s"second access: $flaky")   // 2

  section("def: re-evaluated on every call")
  var n = 0
  def next = { n += 1; n }
  println(s"$next $next $next")   // 1 2 3

  section("val vs lazy val vs def — the evaluation-strategy table")
  //            evaluated    | how often
  // val        at definition| once
  // lazy val   first use    | once (unless it throws)
  // def        each use     | every time
  println("see comment table")

  section("Blocks are expressions; the last expression is the value")
  val area = {
    val w = 3
    val h = 4
    w * h
  }
  println(area)                   // 12
  // Indentation syntax (Scala 3) — a block is just an indented region:
  val area2 =
    val w = 5
    val h = 6
    w * h
  println(area2)                  // 30

  section("Unit and side-effecting expressions")
  val u: Unit = println("an assignment/println returns ()")
  println(u)                      // ()
  // ⚠ assignment returns Unit, so this is NOT a chain assignment:
  var a1 = 0
  var b1 = 0
  val result = { a1 = 5 }         // result: Unit, a1 == 5
  println(s"result=$result a1=$a1 b1=$b1")
  // ⚠ Since named tuples (3.7), `(a1 = 5)` in parentheses is ambiguous with a one-element
  // named tuple; use braces for an assignment-as-expression.
  // ⚠ "value discarding": a non-Unit value where Unit is expected is silently discarded.
  def proc(): Unit = 42           // compiles (warning with -Wvalue-discard)
  println(proc())                 // ()

  section("Destructuring vals (pattern definitions)")
  val (q, r) = (17 / 5, 17 % 5)
  println(s"q=$q r=$r")
  val List(first, second, _*) = List(10, 20, 30, 40): @unchecked
  println(s"first=$first second=$second")
  val Some(found) = Option(1): @unchecked  // ⚠ throws MatchError if None (hence @unchecked)
  println(found)
  // ⚠ Inside a *pattern*, an Uppercase identifier is a constant to compare against,
  // not a new binding:
  val Expected = 17 / 5
  val (Expected, rem) = (3, 2): @unchecked    // checks first == Expected, binds rem
  println(s"matched Expected, rem=$rem")

  section("Multiple definitions & shadowing")
  val x1, x2 = { println("  initializer runs per name"); 7 }   // evaluated twice!
  println(s"$x1 $x2")
  val shadow = 1
  locally {
    val shadow = 2                // inner scope may shadow
    println(s"inner shadow = $shadow")
  }
  println(s"outer shadow = $shadow")
  doesNotCompile("val dup = 1; val dup = 2") // same scope: error

  section("Identifiers")
  val `my weird name!` = "backticks allow any identifier"
  val `type` = "even keywords"
  val unicodeΣ = "unicode letters are letters"
  val +++ = "operator identifiers are legal"
  val name_+ = "mixed: letters then '_' then operator chars"
  println(Seq(`my weird name!`, `type`, unicodeΣ, +++, name_+).mkString("\n"))
  // ⚠ `name_+:` would end with ':' -> right associative when used infix.

  section("Constants: final val and inline val")
  // `final val` with a literal type is a compile-time constant (inlined by the compiler).
  // `inline val` guarantees it.
  println(Consts.Max + Consts.Min)

object Consts:
  final val Max = 100         // type is the literal type 100 → constant-folded
  inline val Min = 1

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
