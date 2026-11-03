//> using scala 3.9.0
// Run: scala-cli run lessons/06-types/10_opaque_types.scala
//
// Opaque type aliases: a NEW type at compile time, the underlying type at runtime.
// Zero allocation (unlike wrapper classes), real type safety (unlike plain aliases).
// Inside the defining scope the alias is transparent; outside it is abstract.

package types.opaques

import scala.compiletime.testing.typeCheckErrors

object Ids:
  opaque type UserId = Long
  opaque type OrderId = Long

  object UserId:
    def apply(l: Long): UserId = l          // inside: UserId = Long, so this is free
    extension (id: UserId) def value: Long = id
  object OrderId:
    def apply(l: Long): OrderId = l

import Ids.*

// Smart constructor with validation
object Emails:
  opaque type Email = String
  object Email:
    def parse(s: String): Either[String, Email] =
      if s.matches("""[^@\s]+@[^@\s]+\.\w+""") then Right(s) else Left(s"invalid email: $s")
    def unsafe(s: String): Email = s
  extension (e: Email)
    def domain: String = e.substring(e.indexOf('@') + 1)
    def show: String = e

import Emails.*

// Bounds: expose SOME of the underlying type's nature
object Money:
  opaque type Cents <: Long = Long          // Cents is known to be a subtype of Long outside
  def cents(l: Long): Cents = l
  // ⚠ Naming this `+` would be useless: since Cents <: Long is visible outside, Long's own
  //   `+` member is found first (members beat extensions) and returns a plain Long.
  extension (c: Cents) def plus(o: Cents): Cents = c + o   // inside: Long + Long

import Money.*

// Units of measure with no runtime cost
object Units:
  opaque type Meters = Double
  opaque type Seconds = Double
  def meters(d: Double): Meters = d
  def seconds(d: Double): Seconds = d
  extension (m: Meters)
    def /(s: Seconds): Double = m / s       // inside: Double / Double
    def plus(o: Meters): Meters = m + o
    def raw: Double = m

import Units.*

// Opaque types can be generic
object NonEmpty:
  opaque type NEList[+A] = List[A]
  def of[A](head: A, tail: A*): NEList[A] = head :: tail.toList
  def from[A](xs: List[A]): Option[NEList[A]] = Option.when(xs.nonEmpty)(xs)
  extension [A](nel: NEList[A])
    def head: A = nel.head                  // safe: never empty
    def toList: List[A] = nel
    def map[B](f: A => B): NEList[B] = nel.map(f)

import NonEmpty.*

@main def run(): Unit =
  section("Distinct types, same runtime representation")
  val u = UserId(42)
  val o = OrderId(42)
  def loadUser(id: UserId) = s"user ${id.value}"
  println(loadUser(u))
  doesNotCompile("types.opaques.Ids.UserId(1): types.opaques.Ids.OrderId")      // different types
  doesNotCompile("val l: Long = types.opaques.Ids.UserId(1)")                    // not a Long outside
  doesNotCompile("types.opaques.Ids.UserId(1) + 1")                              // no Long methods
  println(u.asInstanceOf[AnyRef].getClass)   // java.lang.Long — no wrapper object
  // ⚠ at runtime they're indistinguishable:
  println((u: Any) == (o: Any))              // true

  section("Smart constructors")
  println(Email.parse("ada@lovelace.org").map(_.domain))
  println(Email.parse("nope"))
  doesNotCompile("val e: types.opaques.Emails.Email = \"raw string\"")

  section("Bounded opaque types")
  val c: Cents = cents(150).plus(cents(250))
  val asLong: Long = c                       // OK: Cents <: Long is visible
  println(asLong)
  val sum = cents(1) + cents(2)              // ⚠ Long's `+`: the result is a Long, not Cents
  doesNotCompile("val x: types.opaques.Money.Cents = 5L")   // and Long is not a Cents
  println(sum)

  section("Units of measure")
  val speed = meters(100) / seconds(9.58)
  println(f"$speed%.2f m/s")
  doesNotCompile("types.opaques.Units.meters(1).plus(types.opaques.Units.seconds(1))")
  println(meters(1).plus(meters(2)).raw)

  section("Generic opaque: NonEmptyList")
  val nel = NonEmpty.of(1, 2, 3)
  println(nel.head)
  println(nel.map(_ * 10).toList)
  println(NonEmpty.from(Nil))

  section("⚠ Pitfalls")
  // 1. Pattern matching sees the runtime type — type tests can't distinguish opaque types:
  val any: Any = u
  any match
    case _: Long => println("  it's a Long at runtime (would also match an OrderId)")
    case _       => ()
  // 2. Members beat extensions. Outside the defining scope a plain opaque type has no members,
  //    so its extensions are used. But inside the scope (and outside, for a BOUNDED opaque type
  //    like Cents) the underlying type's members are visible and win over same-named extensions.
  // 3. Opaque types aren't Matchable (matching on them warns under -source future).
  // 4. Implicit search: givens for the opaque type go in its companion (object UserId).
  println("  see comments")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
