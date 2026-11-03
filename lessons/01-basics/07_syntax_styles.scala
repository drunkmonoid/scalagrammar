//> using scala 3.9.0
// Run: scala-cli run lessons/01-basics/07_syntax_styles.scala
//
// Scala 3 accepts two surface syntaxes that can be mixed freely:
//   * classic braces + parentheses (Scala 2 style)
//   * "optional braces" / significant indentation (Scala 3 style)
// The compiler can convert between them:
//   scala-cli compile file.scala -O -rewrite -O -indent      (braces → indentation)
//   scala-cli compile file.scala -O -rewrite -O -no-indent   (indentation → braces)
//   -new-syntax / -old-syntax switch `if (c)` ↔ `if c then`
// Disable indentation syntax completely with -no-indent.

package basics.syntax

// ---------- the same program, two styles ----------

object Braces {
  def classify(n: Int): String = {
    if (n < 0) {
      "negative"
    } else if (n == 0) {
      "zero"
    } else {
      val parity = n % 2 match {
        case 0 => "even"
        case _ => "odd"
      }
      s"positive $parity"
    }
  }
}

object Indented:
  def classify(n: Int): String =
    if n < 0 then
      "negative"
    else if n == 0 then
      "zero"
    else
      val parity = n % 2 match
        case 0 => "even"
        case _ => "odd"
      s"positive $parity"
  end classify          // optional end marker
end Indented

// ---------- rules worth knowing ----------

// A `:` at the end of a line opens an indented template body (class/object/trait/given...)
trait Shape:
  def area: Double

class Circle(r: Double) extends Shape:
  def area: Double = math.Pi * r * r

// End markers can close: class, object, trait, def, val, given, if, while, for, match, try, new, extension, package
class Big:
  def m(): Unit =
    for i <- 1 to 2 do
      println(i)
    end for
    val x =
      try 1
      finally ()
    end x
  end m
end Big

@main def run(): Unit =
  section("Equivalent styles")
  println(Braces.classify(4) == Indented.classify(4))

  section("Control-structure keywords: then / do / yield")
  // `if c then a else b`, `while c do body`, `for x <- xs do body`, `for x <- xs yield e`
  // With parentheses you may omit then/do (Scala 2 style): if (c) a else b
  val c = true
  if c then println("then-style")
  if (c) println("paren-style")
  for (x <- List(1, 2)) print(s"$x ")  // old style
  println()
  for x <- List(1, 2) do print(s"$x ")
  println()

  section("Fewer braces: `:` before a lambda or a block argument")
  val xs = List(1, 2, 3)
  // Instead of xs.map { x => ... } :
  val ys = xs.map: x =>
    val doubled = x * 2
    doubled + 1
  println(ys)
  // A block argument without parameters:
  locally:
    println("inside locally:")
  // Works with multiple args lists too:
  val total = xs.foldLeft(0): (acc, x) =>
    acc + x
  println(total)

  section("Braces are still needed / handy for…")
  // * one-liners with several statements:  { a; b }
  // * lambda arguments in the middle of an expression chain: xs.map { x => ... }.sum
  println(xs.map { x => x * 10 }.sum)
  // * case-lambdas (pattern-matching anonymous functions):
  println(xs.zipWithIndex.map { case (v, i) => v * i })
  // Scala 3 also allows the case-lambda without braces when it's the only argument:
  val sums = xs.zipWithIndex.map:
    case (v, i) => v + i
  println(sums)

  section("Indentation pitfalls")
  // ⚠ Indentation width must be consistent; mixing tabs and spaces is an error.
  // ⚠ A line that is indented MORE than the previous one continues it only when the previous
  //   line ends in something that expects more (an operator, `=`, `then`, `:` etc.).
  def f(x: Int) =
    x
      + 1        // leading infix operator: continues the expression
  println(f(1))  // 2
  // ⚠ This is the classic "dangling expression" trap in braceless code:
  def g(x: Int): Int =
    val y = x * 2
    y            // result
  // a stray statement indented at the method's level would end `g`!
  println(g(5))

  section("Semicolons")
  val a = 1; val b = 2; println(a + b)  // allowed on one line

  section("Parameter-less vs empty-paren methods")
  def now(): Long = System.nanoTime()  // () signals a side-effect
  def size: Int = 3                    // no () → a pure accessor
  println(now() > 0)
  println(size)
  // ⚠ Scala 3 enforces the declared arity when calling methods defined in Scala:
  //   now   (without ()) is an error ("missing argument list") — for Java methods it's allowed
  println("abc".length)                // Java's length() can be called without ()

  section("Universal apply: `new` is optional")
  val sb = java.lang.StringBuilder("x")  // calls the constructor
  println(sb.append("y"))
  val circle = Circle(1.0)               // no `new` needed for any class
  println(circle.area)

def section(title: String): Unit = println(s"\n=== $title ===")
