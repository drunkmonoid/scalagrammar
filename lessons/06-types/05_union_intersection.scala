//> using scala 3.9.0
// Run: scala-cli run lessons/06-types/05_union_intersection.scala
//
// Union types  A | B  — a value that is an A OR a B    (untagged, unlike Either)
// Intersection A & B   — a value that is an A AND a B  (replaces Scala 2's `A with B` types)

package types.unions

import scala.compiletime.testing.typeCheckErrors

case class UserName(value: String)
case class Password(value: String)

def help(id: UserName | Password): String = id match
  case UserName(n) => s"user $n"
  case Password(_) => "a password (hidden)"

// Union types for error channels without a wrapper:
case class NotFound(id: Int)
case class Forbidden(reason: String)
def lookup(id: Int): String | NotFound | Forbidden =
  if id == 0 then Forbidden("root")
  else if id > 100 then NotFound(id)
  else s"item-$id"

trait Resettable:
  def reset(): String = "reset"
trait Growable:
  def grow(): String = "grow"
class Both extends Resettable, Growable

def useBoth(x: Resettable & Growable): String = s"${x.reset()} + ${x.grow()}"

@main def run(): Unit =
  section("Union basics")
  println(help(UserName("ada")))
  println(help(Password("secret")))
  val v: Int | String = if math.random() > 2 then 1 else "s"
  println(v)

  section("Unions are commutative and absorb subtypes")
  val a: Int | String = 1
  val b: String | Int = a                   // A | B =:= B | A
  val c: Any = b
  summon[(Int | String) =:= (String | Int)]
  summon[(Int | Nothing) =:= Int]
  println(s"$b $c")

  section("Exhaustive matching over a union")
  for id <- List(1, 0, 500) do
    lookup(id) match
      case s: String      => println(s"  found $s")
      case NotFound(i)    => println(s"  404 for $i")
      case Forbidden(why) => println(s"  403: $why")

  section("Inference widens unions unless you ask")
  val inferred = if true then 1 else "one"           // Matchable (join), not Int | String
  val kept: Int | String = if true then 1 else "one" // union kept with an annotation
  println(List(inferred, kept))
  val xs = List[Int | String](1, "a")                 // explicit element type
  println(xs)

  section("Members of a union: only common members are available")
  case class Cat(name: String)
  case class Dog(name: String)
  def nameOf(x: Cat | Dog): String = x match       // no common `name` member through the union
    case Cat(n) => n
    case Dog(n) => n
  println(nameOf(Dog("Rex")))
  doesNotCompile("def f(x: Int | String) = x.length")

  section("Union with Null — the explicit-nulls style")
  def safeLen(s: String | Null): Int = if s == null then 0 else s.length   // flow typing narrows s
  println(safeLen(null) -> safeLen("abc"))

  section("Intersection types")
  println(useBoth(Both()))
  val anon: Resettable & Growable = new Resettable with Growable {}
  println(useBoth(anon))
  doesNotCompile("types.unions.useBoth(new types.unions.Resettable {})")
  summon[(Resettable & Growable) =:= (Growable & Resettable)]   // commutative

  section("Intersection of conflicting members")
  trait A { def hello: Any }
  trait B { def hello: String }
  def call(x: A & B): String = x.hello              // member type = Any & String = String
  println(call(new A with B { def hello = "hi" }))

  section("Union vs Either")
  // Either[L, R] is TAGGED: Either[String, String] distinguishes Left("x") from Right("x").
  // String | String is just String — no way to tell which "side" it came from.
  summon[(String | String) =:= String]
  // Unions shine for ad-hoc alternatives; Either for success/failure with combinators.

  section("Laws")
  // | and & are commutative and associative, & distributes over |:
  //   A & (B | C)  =:=  (A & B) | (A & C)
  // and they form a lattice: A <: A | B,  A & B <: A,  Nothing is the unit of |, Any of &.
  summon[Resettable <:< (Resettable | Growable)]
  summon[(Resettable & Growable) <:< Resettable]
  println("compile-time proofs (summon[...]) above passed")

  section("Unions in type parameters & collections")
  val mixed: List[Int | Boolean] = List(1, true, 2)
  val ints = mixed.collect { case i: Int => i }
  println(ints.sum)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
