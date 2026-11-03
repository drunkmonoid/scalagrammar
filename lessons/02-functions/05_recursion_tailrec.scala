//> using scala 3.9.0
// Run: scala-cli run lessons/02-functions/05_recursion_tailrec.scala
//
// Recursion, tail calls, @tailrec, trampolines, and stack safety.

package functions.recursion

import scala.annotation.tailrec
import scala.compiletime.testing.typeCheckErrors
import scala.util.control.TailCalls.*

@main def run(): Unit =
  section("Plain recursion")
  def factorial(n: Int): BigInt = if n <= 1 then 1 else n * factorial(n - 1)
  println(factorial(25))
  // ⚠ not tail recursive: `n * factorial(n - 1)` multiplies AFTER the call returns
  try factorial(100_000)
  catch case _: StackOverflowError => println("StackOverflowError for factorial(100000)")

  section("Tail recursion with an accumulator")
  def factTR(n: Int): BigInt =
    @tailrec def loop(k: Int, acc: BigInt): BigInt =
      if k <= 1 then acc else loop(k - 1, acc * k)   // call is the LAST action
    loop(n, 1)
  println(factTR(20_000).toString.length + " digits")   // no stack overflow

  section("@tailrec makes the compiler check")
  // These errors are reported by the tailrec PHASE, which runs after type checking, so
  // `typeCheckErrors` can't show them here. Real compiler output, for reference:
  //
  //   @tailrec def f(n: Int): Int = if n == 0 then 0 else 1 + f(n - 1)
  //   → Cannot rewrite recursive call: it is not in tail position
  //
  // ⚠ the method must be final or private (or local / in an object) — an overridable
  //   method can't be optimised because a subclass could change the call target:
  //   class C { @tailrec def g(n: Int): Int = if n == 0 then 0 else g(n - 1) }
  //   → TailRec optimisation not applicable, method g is neither private nor final
  //     so can be overridden
  println("see comments (errors from a later compiler phase)")

  section("Mutual recursion is NOT optimised by scalac (JVM has no TCO)")
  def isEven(n: Int): Boolean = if n == 0 then true else isOdd(n - 1)
  def isOdd(n: Int): Boolean = if n == 0 then false else isEven(n - 1)
  println(isEven(10))
  try isEven(1_000_000)
  catch case _: StackOverflowError => println("StackOverflowError for isEven(1000000)")

  section("Trampolines: scala.util.control.TailCalls")
  def even(n: Int): TailRec[Boolean] = if n == 0 then done(true) else tailcall(odd(n - 1))
  def odd(n: Int): TailRec[Boolean] = if n == 0 then done(false) else tailcall(even(n - 1))
  println(even(1_000_000).result)    // heap instead of stack

  // TailRec is a monad: non-tail recursion made stack-safe
  def fibT(n: Int): TailRec[BigInt] =
    if n < 2 then done(BigInt(n))
    else for
      a <- tailcall(fibT(n - 1))
      b <- tailcall(fibT(n - 2))
    yield a + b
  println(fibT(20).result)

  section("Write your own trampoline (enum Trampoline at the bottom)")
  import Trampoline.*
  def countDown(n: Int): Trampoline[String] =
    if n == 0 then Done("liftoff") else More(() => countDown(n - 1))
  println(countDown(1_000_000).run)

  section("Structural recursion over data")
  import Tree.*
  def sum(t: Tree): Int = t match
    case Leaf(v)    => v
    case Node(l, r) => sum(l) + sum(r)
  // Stack-safe version with an explicit stack (useful for degenerate deep trees):
  def sumSafe(t: Tree): Int =
    @tailrec def go(stack: List[Tree], acc: Int): Int = stack match
      case Nil                  => acc
      case Leaf(v) :: rest      => go(rest, acc + v)
      case Node(l, r) :: rest   => go(l :: r :: rest, acc)
    go(List(t), 0)
  val deep = (1 to 100_000).foldLeft[Tree](Leaf(0))((t, i) => Node(t, Leaf(i)))
  try println(sum(deep)) catch case _: StackOverflowError => println("sum: StackOverflowError")
  println(s"sumSafe: ${sumSafe(deep)}")

  section("Increase the stack (escape hatch)")
  // Threads can be created with a bigger stack size:
  var result = BigInt(0)
  val t = Thread(null, () => result = factorial(50_000), "big-stack", 512L * 1024 * 1024)
  t.start(); t.join()
  println(s"factorial(50000) on a 512MB-stack thread: ${result.toString.length} digits")
  // or start the JVM with -Xss: scala-cli run file.scala --java-opt -Xss512m

enum Trampoline[+A]:
  case Done(a: A)
  case More(next: () => Trampoline[A])
  @tailrec final def run: A = this match
    case Done(a) => a
    case More(k) => k().run

enum Tree:
  case Leaf(v: Int)
  case Node(l: Tree, r: Tree)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
