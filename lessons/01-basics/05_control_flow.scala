//> using scala 3.9.0
// Run: scala-cli run lessons/01-basics/05_control_flow.scala
//
// if / while / for / match / try are all EXPRESSIONS. There is no `break`/`continue`
// keyword — see scala.util.boundary. `return` exists but is discouraged.

package basics.controlflow

import scala.util.boundary
import scala.util.boundary.break
import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("if is an expression")
  val x = 7
  val parity = if x % 2 == 0 then "even" else "odd"
  println(parity)
  // Old (Scala 2) syntax still valid:
  val parity2 = if (x % 2 == 0) "even" else "odd"
  println(parity2)
  // if without else has type Unit (the missing branch is `else ()`)
  val noElse = if x > 100 then "big"
  println(s"if without else → $noElse")   // ()
  // else-if chains
  val size = if x < 5 then "small" else if x < 10 then "medium" else "large"
  println(size)

  section("while / do-while")
  var i = 0
  while i < 3 do
    print(s"$i ")
    i += 1
  println()
  // Scala 3 dropped `do { } while (cond)`. Idiom: put the body in the condition.
  var j = 10
  while
    print(s"$j ")   // body runs at least once
    j += 1
    j < 3           // last expression is the condition
  do ()
  println()

  section("for loops (for-do) — side effects")
  for k <- 1 to 3 do print(s"$k ")
  println()
  for k <- 1 until 10 by 3 do print(s"$k ")      // 1 4 7
  println()
  for k <- 10 to 1 by -4 do print(s"$k ")        // 10 6 2
  println()
  // multiple generators = nested loops; guards with `if`; mid-stream definitions with `=`
  for
    a <- 1 to 3
    b <- a to 3
    if (a + b) % 2 == 0
    sum = a + b
  do print(s"($a,$b)=$sum ")
  println()

  section("for-yield — comprehensions produce a value")
  val squares = for n <- List(1, 2, 3) yield n * n
  println(squares)
  // The result collection type follows the FIRST generator:
  println(for n <- Set(1, 2, 3) yield n % 2)     // ⚠ Set(1, 0) — duplicates collapse
  println(for n <- 1 to 3 yield n)               // Vector (IndexedSeq from Range)
  println(for c <- "abc" yield c.toUpper)        // ABC (a String)
  println(for (k, v) <- Map(1 -> "a", 2 -> "b") yield (v, k))  // a Map again
  println(for (k, v) <- Map(1 -> "a", 2 -> "b") yield v)       // ⚠ Iterable, not a Map
  // Pattern in generator acts as a FILTER (refutable patterns are filtered out):
  val mixed: List[Option[Int]] = List(Some(1), None, Some(3))
  println(for case Some(v) <- mixed yield v)     // List(1, 3)  (`case` makes filtering explicit)
  // Desugaring is covered in lessons/11-advanced/01_for_comprehensions.scala.

  section("match is an expression")
  def describe(v: Any): String = v match
    case 0                      => "zero"
    case n: Int if n < 0        => "negative int"
    case n: Int                 => s"int $n"
    case s: String              => s"string of length ${s.length}"
    case (a, b)                 => s"pair $a, $b"
    case xs: List[?]            => s"list of ${xs.size}"
    case _                      => "something else"
  List(0, -3, 5, "hey", (1, 2), List(1, 2), 3.0).foreach(v => println(describe(v)))
  // ⚠ A match with no matching case throws MatchError at runtime:
  try (42: Any) match { case "no" => () }
  catch case e: MatchError => println(s"MatchError: ${e.getMessage}")

  section("try / catch / finally is an expression")
  def parse(s: String): Int =
    try s.toInt
    catch
      case _: NumberFormatException => -1
    finally println(s"  (finally for '$s')")
  println(parse("12"))
  println(parse("zz"))
  // ⚠ the value of `finally` is discarded — it cannot change the result... unless it returns:
  def sneaky(): Int =
    try return 1
    finally return 2   // overrides! (warning) — never do this
  println(s"sneaky() = ${sneaky()}")

  section("No break/continue: boundary & break")
  val firstOver10 = boundary:
    for n <- List(3, 8, 12, 20) do
      if n > 10 then break(n)  // non-local exit with a value
    -1
  println(firstOver10)         // 12
  // "continue": wrap the loop BODY in a boundary
  for n <- 1 to 6 do
    boundary:
      if n % 2 == 0 then break()   // break() = break(()) — skip rest of this iteration
      print(s"$n ")
  println()
  // Labeled boundaries for nested loops:
  val pair = boundary[(Int, Int) | Null]:
    for a <- 1 to 9; b <- 1 to 9 do
      if a * b == 42 then break((a, b))
    null
  println(s"first factor pair of 42: $pair")
  // Legacy: scala.util.control.Breaks (exception-based, slower, easy to misuse)
  import scala.util.control.Breaks.{breakable, break as brk}
  breakable { for n <- 1 to 10 do if n == 3 then brk() else print(s"$n ") }
  println()

  section("return — non-idiomatic and has traps")
  def find(xs: List[Int], p: Int => Boolean): Option[Int] =
    for x <- xs do
      if p(x) then return Some(x)   // ⚠ inside a lambda: a *non-local return* (deprecated)
    None
  println(find(List(1, 2, 3), _ > 1))
  // In Scala 3 non-local returns from lambdas are deprecated: use boundary/break instead.
  doesNotCompile("do println(1) while (false)")   // Scala 2's do-while is gone

  section("Nothing: the type of throw, ???, sys.exit")
  def fail(msg: String): Nothing = throw IllegalStateException(msg)
  val safe: Int = if x > 0 then x else fail("negative") // Nothing conforms to Int
  println(safe)
  def todo: Int = ???                                   // throws NotImplementedError
  try todo catch case e: NotImplementedError => println(s"??? → $e")

  section("Short-circuit operators")
  def loud(b: Boolean): Boolean = { print(s"[eval $b]"); b }
  println(loud(false) && loud(true))   // second never evaluated
  println(loud(true) || loud(false))
  println(loud(false) & loud(true))    // ⚠ & and | are NOT short-circuiting

  section("Infinite loops & Iterator-based control")
  val it = Iterator.iterate(1)(_ * 2).takeWhile(_ < 100)
  println(it.toList)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
