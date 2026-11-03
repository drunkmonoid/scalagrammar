//> using scala 3.9.0
//> using options -language:strictEquality
// Run: scala-cli run lessons/11-advanced/02_multiversal_equality.scala
//
// Universal equality: in Scala 2 (and Scala 3 by default) `a == b` compiles for ANY a and b,
// so comparing apples to oranges silently returns false.
// Multiversal equality (Scala 3): with -language:strictEquality, `==` / `!=` require a
// CanEqual[A, B] instance. This file enables it for the whole file via a compiler option.

package advanced.equality

import scala.compiletime.testing.typeCheckErrors

case class UserId(value: Int) derives CanEqual        // UserId == UserId allowed
case class OrderId(value: Int) derives CanEqual
case class Unrelated(x: Int)                          // no CanEqual: can't even compare to itself

// Allowing a cross-type comparison explicitly:
case class Meters(v: Double) derives CanEqual
case class Feet(v: Double) derives CanEqual
given CanEqual[Meters, Feet] = CanEqual.derived
given CanEqual[Feet, Meters] = CanEqual.derived

@main def run(): Unit =
  section("Comparisons that make sense still work")
  println(UserId(1) == UserId(1))
  println(1 == 1)                                     // primitives and String have CanEqual instances
  println("a" == "a")
  println(List(1, 2) == List(1, 2))                   // lifted: CanEqual[List[A], List[B]] if CanEqual[A, B]
  println(Option(1) == Option(2))

  section("Bugs caught at compile time")
  println(typeCheckErrors("advanced.equality.UserId(1) == advanced.equality.OrderId(1)").map(_.message.linesIterator.next()))
  println(typeCheckErrors("advanced.equality.UserId(1) == 1").map(_.message.linesIterator.next()))
  println(typeCheckErrors("\"1\" == 1").map(_.message.linesIterator.next()))
  println(typeCheckErrors("advanced.equality.Unrelated(1) == advanced.equality.Unrelated(1)").map(_.message.linesIterator.next()))

  section("Explicit cross-type equality")
  println(Meters(1) == Feet(1))                       // compiles (our givens), but false: different classes

  section("Pattern matching")
  // Constructor and type patterns don't call ==. Literal and stable-identifier patterns do,
  // so under strictEquality they need a CanEqual between the scrutinee and the pattern.
  val id = UserId(7)
  id match
    case UserId(7) => println("  constructor pattern with an Int literal inside: fine")
    case _         => ()
  val expected = UserId(7)
  id match
    case `expected` => println("  stable identifier pattern: uses CanEqual[UserId, UserId]")
    case _          => ()

  section("Without strictEquality")
  // Remove the `using options` line at the top: every comparison above compiles, and
  // UserId(1) == OrderId(1) silently returns false.
  println("see comments")

def section(title: String): Unit = println(s"\n=== $title ===")
