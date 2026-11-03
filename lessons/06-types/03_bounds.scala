//> using scala 3.9.0
// Run: scala-cli run lessons/06-types/03_bounds.scala
//
// Type bounds:
//   A <: Upper        upper bound      ("A is some subtype of Upper")
//   A >: Lower        lower bound      ("A is some supertype of Lower")
//   A >: L <: U       both
//   A : TC            context bound    (sugar for (using TC[A]) — see lesson 07)
//   A <: Comparable[A]  F-bounded       (A refers to itself)

package types.bounds

import scala.compiletime.testing.typeCheckErrors

trait Shape:
  def area: Double
case class Sq(side: Double) extends Shape:
  def area = side * side
case class Circ(r: Double) extends Shape:
  def area = math.Pi * r * r

// Upper bound: we can call Shape members on A, and we keep the precise type A.
def largest[A <: Shape](xs: List[A]): A = xs.maxBy(_.area)
// Compare: without a type parameter we'd lose the precise type
def largestShape(xs: List[Shape]): Shape = xs.maxBy(_.area)

// Lower bound: widen the element type when adding
def prepend[A, B >: A](x: B, xs: List[A]): List[B] = x :: xs

// F-bounded polymorphism: methods that must return "my own type"
trait Ordered2[A <: Ordered2[A]]:
  def compareTo(that: A): Int
  def <(that: A): Boolean = compareTo(that) < 0
case class Version(major: Int, minor: Int) extends Ordered2[Version]:
  def compareTo(that: Version) = Ordering[(Int, Int)].compare((major, minor), (that.major, that.minor))

trait Builder[Self <: Builder[Self]]:
  self: Self =>
  def name(n: String): Self
case class UserBuilder(n: String = "", admin: Boolean = false) extends Builder[UserBuilder]:
  def name(n: String) = copy(n = n)
  def asAdmin = copy(admin = true)

case class Impostor(x: Int) extends Ordered2[Version]:
  def compareTo(that: Version) = -1

def samePair[A, B](a: A, b: B)(using ev: B =:= A): List[A] = List(a, ev(b))

// Both bounds
def clamp[A >: Sq <: Shape](x: A): A = x

// Context bound (preview): A must have an Ordering
def maxOf[A: Ordering](xs: List[A]): A = xs.max
// Multiple context bounds (Scala 3.6+ syntax `{Ordering, Numeric}`; older: `A: Ordering: Numeric`)
def sumAndMax[A: Numeric](xs: List[A]): (A, A) = (xs.sum, xs.max(using summon[Numeric[A]]))

@main def run(): Unit =
  section("Upper bounds keep precise types")
  val squares = List(Sq(1), Sq(3), Sq(2))
  val big: Sq = largest(squares)              // A = Sq — we get a Sq back
  println(big.side)
  val bigShape = largestShape(squares)        // only a Shape
  doesNotCompile("types.bounds.largestShape(List(types.bounds.Sq(1))).side")
  doesNotCompile("types.bounds.largest(List(1, 2))")   // Int is not a Shape
  println(largest(List(Sq(1), Circ(1))))      // A inferred as Shape (the LUB) — fine
  println(bigShape)

  section("Lower bounds widen")
  val xs: List[Sq] = List(Sq(1))
  val ys = prepend(Circ(1), xs)               // B = Shape (LUB of Circ and Sq) — hover to see
  println(ys)
  val zs = prepend("str", xs)                 // B = Matchable/Any-ish — anything goes
  println(zs)

  section("F-bounded polymorphism")
  println(Version(1, 2) < Version(1, 10))
  // ⚠ F-bounds don't force A to be the implementing class. `Impostor` (below) extends
  //   Ordered2[Version] legally, because Version satisfies the bound:
  println(Impostor(1) < Version(1, 0))
  // A self type (`self: Self =>`, as in Builder) closes that hole:
  println(UserBuilder().name("ada").asAdmin)
  doesNotCompile("case class Bad() extends types.bounds.Builder[types.bounds.UserBuilder] { def name(n: String) = types.bounds.UserBuilder() }")

  section("Lower+upper")
  println(clamp(Sq(2)))
  println(clamp[Shape](Circ(1)))
  doesNotCompile("types.bounds.clamp[types.bounds.Circ](types.bounds.Circ(1))")  // Circ is not >: Sq

  section("Context bounds")
  println(maxOf(List(3, 9, 2)))
  println(maxOf(List("b", "a")))
  println(sumAndMax(List(1.5, 2.5)))
  doesNotCompile("types.bounds.maxOf(List(new Object))")   // no Ordering[Object]

  section("Bounds on type members (preview of 04)")
  trait Container:
    type Elem <: AnyVal
    def get: Elem
  val c = new Container { type Elem = Int; def get = 42 }
  println(c.get)

  section("⚠ Inference picks the LUB, which may be surprisingly wide")
  def pair[A](a: A, b: A): List[A] = List(a, b)
  val p = pair(1, "one")                      // A = Int | String widened → Matchable
  println(p)
  // To forbid that, infer the two types separately and demand =:= evidence:
  println(samePair(1, 2))
  doesNotCompile("types.bounds.samePair(1, \"one\")")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("compiles (no error)")}")
