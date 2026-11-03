//> using scala 3.9.0
// Run: scala-cli run lessons/03-oop/05_inheritance.scala
//
// Inheritance: override rules, abstract members, final/sealed/open, initialization
// order, uniform access principle, and polymorphism corner cases.

package oop.inheritance

import scala.compiletime.testing.typeCheckErrors

abstract class Vehicle(val wheels: Int):
  def name: String                       // abstract def
  val maxSpeed: Int                      // abstract val
  def describe = s"$name: $wheels wheels, max $maxSpeed km/h"

class Car extends Vehicle(4):
  def name = "car"                       // implementing abstract: `override` optional
  val maxSpeed = 200

class SportsCar extends Car:
  override def name = "sports car"       // overriding concrete: `override` REQUIRED
  override val maxSpeed = 300

final class Bicycle extends Vehicle(2):
  val name = "bicycle"                   // a val may implement a def (uniform access)
  val maxSpeed = 40

// `open` signals a class is designed for extension. Extending a non-open, non-abstract class
// from ANOTHER FILE triggers a feature warning (under -source future / with -feature).
open class Plugin:
  def run(): String = "base plugin"

// Sealed: all direct subclasses must be in the same file → exhaustiveness checks.
sealed abstract class Expr
case class Num(n: Int) extends Expr
case class Add(a: Expr, b: Expr) extends Expr

@main def run(): Unit =
  section("Dynamic dispatch")
  val vs: List[Vehicle] = List(Car(), SportsCar(), Bicycle())
  vs.foreach(v => println(v.describe))

  section("Override rules")
  doesNotCompile("class C2 extends Car { def name = \"x\" }")         // missing `override`
  doesNotCompile("class C3 extends Car { override def nmae = \"x\" }")// overrides nothing
  doesNotCompile("class C4 extends Bicycle")                            // final class
  doesNotCompile("class C5 extends Car { override val name = \"x\"; override def maxSpeed = 1 }") // def can't override val
  // A val can override a def (it becomes stable), but a def can't override a val.
  // A lazy val can only be overridden by a lazy val:
  doesNotCompile("class V1 { lazy val x = 1 }; class V2 extends V1 { override val x = 2 }")

  section("Covariant return types & parameter types")
  class Animal:
    def reproduce(): Animal = Animal()
  class Cat extends Animal:
    override def reproduce(): Cat = Cat()    // narrower return type: OK
  println(Cat().reproduce().getClass.getSimpleName)
  // Parameter types must match exactly — otherwise it's an overload, not an override:
  doesNotCompile("class An; class Ca extends An; class A1 { def f(x: Ca) = 1 }; class A2 extends A1 { override def f(x: An) = 2 }")

  section("Fields vs methods: static vs dynamic dispatch")
  class Parent:
    val tag = "parent-val"
    def show = s"show sees $tag"
  class Child extends Parent:
    override val tag = "child-val"
  println(Child().show)                      // child-val (vals are accessed via getters → dynamic)

  section("⚠ Initialization order: superclass body runs FIRST")
  abstract class Base:
    val greeting: String
    val upper: String = if greeting == null then "<null!>" else greeting.toUpperCase
    println(s"  Base init sees greeting=$greeting")
  class Impl extends Base:
    val greeting = "hello"
  println(s"upper = ${Impl().upper}")        // <null!>
  // Fixes: lazy val, def, constructor parameter, or early initialization via trait params.
  abstract class Base2(val greeting: String):
    val upper: String = greeting.toUpperCase
  class Impl2 extends Base2("hello")
  println(s"upper = ${Impl2().upper}")

  section("⚠ Calling overridable methods from a constructor")
  class Widget:
    init()
    def init(): Unit = println("  Widget.init")
  class Button extends Widget:
    val label = "OK"
    override def init(): Unit = println(s"  Button.init sees label=$label")   // null!
  Button()

  section("super calls")
  class Logger:
    def log(m: String) = s"[base] $m"
  class TimestampLogger extends Logger:
    override def log(m: String) = super.log(s"t=0 $m")
  println(TimestampLogger().log("hi"))

  section("Sealed hierarchies & exhaustiveness")
  def eval(e: Expr): Int = e match
    case Num(n)    => n
    case Add(a, b) => eval(a) + eval(b)
  println(eval(Add(Num(1), Add(Num(2), Num(3)))))
  // Removing a case would produce a compile-time WARNING "match may not be exhaustive".

  section("Type tests & casts")
  val v: Vehicle = SportsCar()
  println(v.isInstanceOf[Car])               // true
  println(v.asInstanceOf[Car].maxSpeed)      // 300
  try v.asInstanceOf[Bicycle]
  catch case e: ClassCastException => println(s"ClassCastException: ${e.getMessage.take(60)}...")
  // Prefer pattern matching:
  v match
    case c: Car => println(s"it's a car-like: ${c.name}")
    case _      => ()

  section("Any, AnyRef, Matchable — the roots")
  // Any
  //  ├─ Matchable
  //  │   ├─ AnyVal (Int, Double, Boolean, Unit, ... value classes)
  //  │   └─ AnyRef (= java.lang.Object; all classes)
  //  └─ (universal traits, opaque types — not Matchable)
  // Nothing is a subtype of everything; Null is a subtype of all reference types.
  println(classOf[SportsCar].getSuperclass.getSimpleName)
  println(classOf[Car].getSuperclass.getSimpleName)
  println(classOf[Vehicle].getSuperclass.getSimpleName)  // Object

  section("Protected")
  class P:
    protected def secret = 42
    def compare(other: P) = other.secret        // OK: same class
  class Q extends P:
    def reveal = secret                         // OK: subclass
  println(Q().reveal)
  doesNotCompile("class P0 { protected def s = 1 }; new P0().s")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
