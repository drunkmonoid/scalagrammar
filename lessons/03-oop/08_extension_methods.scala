//> using scala 3.9.0
// Run: scala-cli run lessons/03-oop/08_extension_methods.scala
//
// Extension methods add methods to existing types without wrappers or inheritance.
// (Scala 2 used `implicit class` for this — see lessons/12-scala2.)

package oop.extensions

import scala.compiletime.testing.typeCheckErrors

case class Circle(x: Double, y: Double, radius: Double)

// Single extension
extension (c: Circle)
  def circumference: Double = c.radius * math.Pi * 2

// Collective extension: several methods sharing the receiver
extension (s: String)
  def shout: String = s.toUpperCase + "!"
  def words: List[String] = s.split("\\s+").toList.filter(_.nonEmpty)
  def wordCount: Int = words.length       // other extension methods are in scope unqualified
  infix def repeatWith(sep: String): String = s"$s$sep$s"

// Generic extensions, with type parameters and context bounds
extension [A](xs: List[A])
  def second: Option[A] = xs.drop(1).headOption
  def sumBy[B: Numeric](f: A => B): B = xs.map(f).sum

extension [A: Ordering](xs: List[A])
  def isSorted: Boolean = xs.zip(xs.drop(1)).forall((a, b) => summon[Ordering[A]].lteq(a, b))

// Operators as extensions; right-associative ones swap the receiver!
extension (n: Int)
  def ** (exp: Int): Int = math.pow(n, exp).toInt
  def times(body: => Unit): Unit = for _ <- 1 to n do body

extension [A](a: A)
  def ::> (xs: List[A]): List[A] = xs :+ a    // left-assoc: `a ::> xs`
  // Ends in ':' → right-associative. Unlike ordinary right-assoc METHODS (where the receiver
  // is the RIGHT operand), for extensions the extension parameter is always the operand
  // written on the LEFT, so the definition reads like the use site: `a +:: xs`.
  def +:: (xs: List[A]): List[A] = a :: xs

// Extensions in objects must be imported (or brought in via givens/companions)
object StringSyntax:
  extension (s: String) def isPalindrome: Boolean =
    val clean = s.toLowerCase.filter(_.isLetterOrDigit)
    clean == clean.reverse

// Extensions in a companion/given are found automatically through the implicit scope
case class Money(cents: Long)
object Money:
  extension (m: Money)
    def +(o: Money): Money = Money(m.cents + o.cents)
    def show: String = f"$$${m.cents / 100.0}%.2f"

// Type-class style: extension methods defined inside a trait, provided via a given
trait Show[A]:
  extension (a: A) def show: String
given Show[Int] with
  extension (a: Int) def show: String = s"Int($a)"

// Overloaded extensions (must be top-level/members: local defs can't be overloaded)
extension (i: Int) def describe: String = s"int $i"
extension (s: String) def describe: String = s"string $s"

@main def run(): Unit =
  section("Basic use")
  println(Circle(0, 0, 1).circumference)
  println("hello scala".shout)
  println("  many   words here ".wordCount)
  println("ab" repeatWith "|")

  section("Generic extensions")
  println(List(1, 2, 3).second)
  println(List("a", "bb").sumBy(_.length))
  println(List(1, 2, 3).isSorted -> List(3, 1).isSorted)

  section("Operators")
  println(2 ** 10)
  3.times(print("x"))
  println()
  println(0 ::> List(1, 2))     // List(1, 2, 0)
  println(0 +:: List(1, 2))     // List(0, 1, 2)
  println(1 +:: 2 +:: List(3))  // List(1, 2, 3) — groups as 1 +:: (2 +:: List(3))

  section("Explicit invocation")
  println(circumference(Circle(0, 0, 2)))   // extension methods are ordinary methods too
  println(shout("direct call"))

  section("Imports and scoping")
  doesNotCompile("\"racecar\".isPalindrome")
  import StringSyntax.*
  println("A man, a plan, a canal: Panama".isPalindrome)

  section("Found via companion (implicit scope)")
  println((Money(150) + Money(275)).show)   // no import needed

  section("Type-class extension via given")
  println(42.show)

  section("⚠ Members always win over extensions")
  extension (s: String) def length: Int = -1   // never chosen: String already has `length`
  println("abc".length)                        // 3
  // An extension is only tried when normal member lookup FAILS.

  section("⚠ Extensions can't access private members")
  doesNotCompile("extension (x: oop.extensions.Secret) def peek = x.hidden")

  section("Overloaded extension resolution")
  println(1.describe)
  println("a".describe)

class Secret:
  private val hidden = 1

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
