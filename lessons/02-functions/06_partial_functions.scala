//> using scala 3.9.0
// Run: scala-cli run lessons/02-functions/06_partial_functions.scala
//
// PartialFunction[A, B]: a function defined only for some inputs (isDefinedAt).
// Not to be confused with *partially applied* functions (lesson 03).

package functions.partial

@main def run(): Unit =
  section("Defining a PartialFunction with case clauses")
  val sqrt: PartialFunction[Double, Double] = {
    case x if x >= 0 => math.sqrt(x)
  }
  println(sqrt.isDefinedAt(4))     // true
  println(sqrt.isDefinedAt(-4))    // false
  println(sqrt(16))
  try sqrt(-1) catch case e: MatchError => println(s"outside the domain → MatchError")

  section("collect = filter + map in one pass")
  val mixed: List[Any] = List(1, "two", 3.0, 4, "five")
  println(mixed.collect { case i: Int => i * 10 })
  println(mixed.collectFirst { case s: String if s.length > 3 => s })

  section("Combining: orElse, andThen, lift, applyOrElse")
  val small: PartialFunction[Int, String] = { case n if n < 10 => s"small $n" }
  val big: PartialFunction[Int, String] = { case n if n >= 100 => s"big $n" }
  val both = small orElse big
  println(List(1, 50, 500).map(both.lift))   // List(Some(small 1), None, Some(big 500))
  println(both.applyOrElse(50, (n: Int) => s"medium $n"))
  val shout = both andThen (_.toUpperCase)
  println(shout(5))
  // unlift turns A => Option[B] into a PartialFunction
  val parse: PartialFunction[String, Int] = Function.unlift((s: String) => s.toIntOption)
  println(List("1", "x", "3").collect(parse))
  // Scala 2.13+: `.unlift` on functions returning Option
  println(((s: String) => s.toIntOption).unlift.isDefinedAt("7"))

  section("Pattern-matching anonymous functions are ALSO plain functions")
  // `{ case ... }` adapts to Function1 when the expected type is a Function1 (no isDefinedAt).
  val f: Int => String = { case 0 => "zero"; case _ => "other" }
  println(f(0))
  // ⚠ A non-exhaustive case lambda typed as Function1 throws MatchError on unmatched input.
  val g: Int => String = { case 1 => "one" }
  try g(2) catch case _: MatchError => println("g(2) → MatchError")

  section("⚠ Evaluation trap: isDefinedAt + apply runs guards twice")
  var guardRuns = 0
  val pf: PartialFunction[Int, Int] = { case x if { guardRuns += 1; x > 0 } => x }
  if pf.isDefinedAt(1) then pf(1)
  println(s"guard ran $guardRuns times")    // 2
  guardRuns = 0
  pf.applyOrElse(1, _ => 0)                 // applyOrElse is optimised: runs pattern once
  println(s"with applyOrElse: $guardRuns time(s)")

  section("Partial functions in the standard library")
  // Map is a PartialFunction[K, V]!
  val m = Map("a" -> 1, "b" -> 2)
  println(List("a", "z", "b").collect(m))   // List(1, 2)
  // Seq is a PartialFunction[Int, A] (index → element)
  val seq = Vector("x", "y")
  println(List(0, 5, 1).collect(seq))       // List(x, y)
  println(seq.lift(10))                     // None — safe indexing
  // Actor-style message handlers (e.g. Pekko classic `receive`) are PartialFunction[Any, Unit].
  // try/catch handlers are partial functions too:
  val handler: PartialFunction[Throwable, String] = { case _: ArithmeticException => "math!" }
  println(try (1 / 0).toString catch handler)

def section(title: String): Unit = println(s"\n=== $title ===")
