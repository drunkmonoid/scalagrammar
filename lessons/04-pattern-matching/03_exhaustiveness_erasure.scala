//> using scala 3.9.0
// Run: scala-cli run lessons/04-pattern-matching/03_exhaustiveness_erasure.scala
//
// What the compiler can and cannot check:
//   * exhaustiveness (sealed hierarchies, enums, Boolean, tuples of them, unions)
//   * unreachable cases
//   * type erasure: generic type arguments are NOT available at runtime
//   * Matchable: you can't pattern match on values of unknown/abstract types freely
//   * TypeTest / ClassTag: recovering runtime type checks for abstract types

package patterns.exhaustiveness

import scala.reflect.{ClassTag, TypeTest}
import scala.compiletime.testing.typeCheckErrors

sealed trait Light
case object Red extends Light
case object Yellow extends Light
case object Green extends Light

@main def run(): Unit =
  section("Exhaustiveness warnings")
  // This compiles with: "match may not be exhaustive. It would fail on pattern case: Green"
  // (a WARNING by default — make it fatal with -Werror or -Xfatal-warnings)
  def next(l: Light): Light = (l: @unchecked) match   // @unchecked silences it
    case Red    => Green
    case Yellow => Red
  println(next(Red))
  try next(Green) catch case e: MatchError => println(s"  MatchError: $e")

  // Tuples of sealed types are checked combinatorially:
  def both(a: Boolean, b: Boolean) = (a, b) match
    case (true, true)   => "both"
    case (false, false) => "neither"
    case (true, false) | (false, true) => "one"
  println(both(true, false))

  section("Guards defeat exhaustiveness analysis")
  def sign(n: Int) = n match
    case x if x > 0 => "+"
    case x if x < 0 => "-"
    case _          => "0"  // needed: the compiler can't prove the guards cover all ints
  println(sign(0))

  section("Unreachable cases (warnings)")
  def unreachable(x: Any) = x match
    case _: String       => "string"
    case _: CharSequence => "char sequence (String already handled)"
    case _               => "other"
  println(unreachable(new java.lang.StringBuilder("x")))

  section("Type erasure")
  def erased(x: Any) = x match
    case _: List[Int] @unchecked => "List[Int]?"   // only List is checked!
    case _                       => "other"
  println(erased(List("not", "ints")))    // ⚠ "List[Int]?" — the Int is erased
  // Without @unchecked the compiler warns:
  // "the type test for List[Int] cannot be checked at runtime because its type arguments can't be determined"
  // Use wildcards for honesty:
  def honest(x: Any) = x match
    case xs: List[?] => s"some list of size ${xs.size}"
    case _           => "other"
  println(honest(List(1)))
  // Check elements yourself if you need to:
  def intList(x: Any): Option[List[Int]] = x match
    case xs: List[?] if xs.forall(_.isInstanceOf[Int]) => Some(xs.asInstanceOf[List[Int]])
    case _                                              => None
  println(intList(List(1, 2)) -> intList(List("a")))
  // Arrays are NOT erased (JVM arrays are reified):
  def arr(x: Any) = x match
    case _: Array[Int]    => "Array[Int]"
    case _: Array[String] => "Array[String]"
    case _                => "other"
  println(arr(Array("a")))

  section("Abstract type patterns: ClassTag & TypeTest")
  // ⚠ `case a: A` for a type parameter A is unchecked (always succeeds after erasure):
  def firstOfNaive[A](xs: List[Any]): Option[A] = xs.collectFirst { case a: A @unchecked => a }
  println(firstOfNaive[String](List(1, "two")))  // ⚠ Some(1) — WRONG
  // With a ClassTag the compiler inserts a real runtime check:
  def firstOf[A: ClassTag](xs: List[Any]): Option[A] = xs.collectFirst { case a: A => a }
  println(firstOf[String](List(1, "two")))       // Some(two)
  println(firstOf[Int](List("x", 3)))            // Some(3) — ClassTag[Int] handles boxing
  // TypeTest generalises ClassTag (can check types that aren't simple classes, e.g. opaque types)
  def only[A](xs: List[Any])(using tt: TypeTest[Any, A]): List[A] = xs.collect { case tt(a) => a }
  println(only[String](List(1, "a", 2.0, "b")))

  section("Matchable: matching on unconstrained type params")
  // In Scala 3, a value of type `A` (unbounded) might be an opaque type alias, so
  // matching on its runtime type is flagged (under -source future):
  def describe[A <: Matchable](a: A) = a match   // bound A <: Matchable to opt in
    case i: Int => s"int $i"
    case _      => "something else"
  println(describe(1))

  section("Union & enum exhaustiveness")
  def u(x: Int | String) = x match
    case _: Int    => "int"
    case _: String => "string"       // exhaustive, no default needed
  println(u("s"))

  section("Refutable patterns in val / for")
  compileCheck("val Some(x) = Option(1)")
  // Scala 3 warns: "pattern's type Some[Int] is more specialized than the right hand side
  // expression's type Option[Int]". Add `: @unchecked` to the right-hand side to opt out,
  // or use `for case Some(x) <- ...` in comprehensions to filter instead.
  compileCheck("val x :: Nil = List(1)")
  compileCheck("Option(1) match { case Some(x) => x }")   // warns: not exhaustive

def section(title: String): Unit = println(s"\n=== $title ===")

// Prints the first compile ERROR, or notes that the code compiles (warnings aren't errors).
inline def compileCheck(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  `$code`\n      → ${msg.getOrElse("compiles (possibly with a warning)")}")
