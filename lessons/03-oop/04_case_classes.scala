//> using scala 3.9.0
// Run: scala-cli run lessons/03-oop/04_case_classes.scala
//
// Case classes: immutable data with generated equals/hashCode/toString/copy/unapply,
// and the many corner cases around them.

package oop.caseclasses

import scala.compiletime.testing.typeCheckErrors

case class Person(name: String, age: Int)

@main def run(): Unit =
  section("What you get for free")
  val ada = Person("Ada", 36)           // apply in companion (no `new`)
  println(ada)                          // toString: Person(Ada,36)
  println(ada.name)                     // params are public vals
  println(ada == Person("Ada", 36))     // structural equality
  println(ada.hashCode == Person("Ada", 36).hashCode)
  println(ada.copy(age = 37))           // copy with named overrides
  val Person(n, a) = ada                // unapply → destructuring
  println(s"$n is $a")
  println(ada.productArity)             // 2  — case classes are Products
  println(ada.productElementNames.toList)
  println(ada.productIterator.toList)
  // ⚠ In Scala 2 the companion extended Function2, so `Person.tupled` worked. Not in Scala 3:
  doesNotCompile("Person.tupled((\"Bob\", 1))")
  println(Tuple.fromProductTyped(ada))  // (Ada,36)
  println(summon[deriving.Mirror.ProductOf[Person]].fromProduct(("Cy", 2)))

  section("Pattern matching")
  ada match
    case Person("Ada", age) if age > 30 => println(s"Ada over 30: $age")
    case Person(_, _)                    => println("someone else")

  section("Equality only considers the FIRST parameter list")
  case class Tagged(id: Int)(val tag: String)
  println(Tagged(1)("a") == Tagged(1)("b"))   // ⚠ true — tag is ignored
  println(Tagged(1)("a"))                     // Tagged(1) — not in toString either
  println(Tagged(1)("a").copy()("c").tag)     // copy must re-supply the second list

  section("Members in the body are NOT part of equality")
  case class Doc(title: String):
    var views = 0
  val d1 = Doc("x"); d1.views = 10
  println(d1 == Doc("x"))                     // ⚠ true

  section("⚠ Mutable fields break hash-based collections")
  case class MutBox(var v: Int)
  val box = MutBox(1)
  val set = collection.mutable.HashSet(box)
  box.v = 2
  println(set.contains(box))                  // ⚠ false — hash changed after insertion
  println(set.exists(_ eq box))               // true — it's still in there!

  section("⚠ Arrays inside case classes compare by reference")
  case class WithArray(xs: Array[Int])
  println(WithArray(Array(1)) == WithArray(Array(1)))   // false
  case class WithSeq(xs: Seq[Int])
  println(WithSeq(Seq(1)) == WithSeq(Vector(1)))        // true (Seq equality is by elements)

  section("Customising: private constructor, validated apply")
  println(Email.parse("a@b.com"))
  println(Email.parse("nope"))
  doesNotCompile("Email(\"x\")")                        // apply is private too (Scala 3)
  doesNotCompile("Email.parse(\"a@b.c\").get.copy(\"x\")") // ...and so is copy

  section("Overriding generated members")
  case class Secret(value: String):
    override def toString = "Secret(****)"
  println(Secret("hunter2"))

  section("Case class inheritance")
  // case-to-case inheritance is prohibited (equality would be broken).
  doesNotCompile("case class P1(x: Int); case class P2(y: Int) extends P1(y)")
  // Normal pattern: sealed trait + case classes (an ADT; see 06_enums_adts)
  sealed trait Shape
  case class Circle(r: Double) extends Shape
  case class Rect(w: Double, h: Double) extends Shape
  val shapes: List[Shape] = List(Circle(1), Rect(2, 3))
  println(shapes.map {
    case Circle(r)  => math.Pi * r * r
    case Rect(w, h) => w * h
  })

  section("Case objects & parameterless case classes")
  doesNotCompile("case class Empty")    // must have a parameter list
  case class Unit_()                    // allowed but usually you want a case object
  println(Unit_() == Unit_())

  section("Variance & type parameters")
  case class Box[+A](value: A)
  val b: Box[Any] = Box[Int](1)        // covariant
  println(b)

  section("Generated members in the companion")
  // The compiler adds `apply`, `unapply` (returns the case class itself in Scala 3!) and
  // a Mirror. In Scala 3, unapply(p: Person): Person — no Option allocation.
  val u = Person.unapply(ada)
  println(u)                           // Person(Ada,36)
  // If you define your own companion, the generated members are still added:
  println(Version(1, 2))
  println(Version.fromString("3.4"))

  section("Tuples are case classes too (and vice versa via Mirror)")
  val t = ("Ada", 36)
  println(t._1 + " " + t(1))           // _1 or apply(index) in Scala 3
  val fromTuple = Person.apply.tupled(t)
  println(fromTuple)

  section("Deconstruction with named fields (Scala 3.7+ named tuples)")
  ada match
    case Person(age = a) => println(s"matched by field name: age=$a")

case class Email private (value: String)
object Email:
  def parse(s: String): Option[Email] =
    if s.contains("@") then Some(new Email(s)) else None

case class Version(major: Int, minor: Int)
object Version:
  def fromString(s: String): Version =
    val Array(a, b) = s.split('.').map(_.toInt): @unchecked
    Version(a, b)                      // generated apply still available

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
