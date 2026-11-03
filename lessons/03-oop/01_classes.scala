//> using scala 3.9.0
// Run: scala-cli run lessons/03-oop/01_classes.scala
//
// Classes: primary constructors, fields vs parameters, auxiliary constructors,
// getters/setters, universal apply, and equality defaults.

package oop.classes

import scala.compiletime.testing.typeCheckErrors

// The class body IS the primary constructor. Parameters:
//   plain `x: Int`   → constructor parameter (becomes a private field only if used in methods)
//   `val x: Int`     → public read-only field
//   `var x: Int`     → public read-write field
//   `private val`    → private field
class Point(val x: Int, var y: Int, z: Int):
  println(s"  constructing Point($x, $y, $z)")   // runs on every `new`
  def sumWithZ: Int = x + y + z                   // z is captured → becomes a private field
  override def toString = s"Point($x, $y)"

class Temperature(private var celsius: Double):
  // Custom getter/setter: `name` and `name_=`
  def fahrenheit: Double = celsius * 9 / 5 + 32
  def fahrenheit_=(f: Double): Unit = celsius = (f - 32) * 5 / 9
  override def toString = f"$celsius%.1f°C"

class Person(val name: String, val age: Int):
  // Auxiliary constructors: `def this(...)`, must call another constructor FIRST
  def this(name: String) = this(name, 0)
  def this() = this("anonymous")

  // Validation in the constructor body
  require(age >= 0, s"age must be >= 0, was $age")
  override def toString = s"Person($name, $age)"

class Counter:
  private var count = 0          // field initialised in the body
  def increment(): this.type =   // returning this.type enables fluent chaining in subclasses
    count += 1
    this
  def current: Int = count

// Default constructor arguments and private constructors
class Connection private (val url: String, val timeout: Int = 30)
object Connection:
  def to(host: String): Connection = new Connection(s"jdbc://$host")

// Abstract classes may have constructor params; traits can too in Scala 3 (see 03_traits)
abstract class Animal(val name: String):
  def sound: String
  def speak: String = s"$name says $sound"

class Dog(name: String) extends Animal(name):
  def sound = "woof"

@main def run(): Unit =
  section("Constructing")
  val p = Point(1, 2, 3)          // universal apply: no `new` needed (Scala 3)
  val p2 = new Point(1, 2, 3)     // still allowed
  println(p.x)
  p.y = 20                        // var → setter
  println(p)
  println(p.sumWithZ)
  doesNotCompile("Point(1, 2, 3).z")   // plain param isn't a public member
  doesNotCompile("Point(1, 2, 3).x = 5")

  section("Getters & setters")
  val t = Temperature(100)
  println(t.fahrenheit)
  t.fahrenheit = 32               // calls fahrenheit_=(32)
  println(t)

  section("Auxiliary constructors")
  println(Person("Ada", 36))
  println(Person("Bob"))
  println(Person())
  try Person("X", -1)
  catch case e: IllegalArgumentException => println(e.getMessage)

  section("Fluent this.type")
  println(Counter().increment().increment().current)

  section("Private constructor + factory")
  val c = Connection.to("db")
  println(c.url -> c.timeout)
  doesNotCompile("new Connection(\"x\")")

  section("Abstract classes & subclassing")
  println(Dog("Rex").speak)
  doesNotCompile("new Animal(\"x\")")
  // Anonymous subclass:
  val cat = new Animal("Tom") { def sound = "meow" }
  println(cat.speak)

  section("Default equality is reference equality (for non-case classes)")
  println(p == p2)                // false — Point doesn't override equals
  println(p == p)                 // true
  println(p.hashCode == p2.hashCode)  // almost certainly false (identity hash)

  section("Implementing equals/hashCode correctly")
  class Money(val amount: BigDecimal, val currency: String):
    override def equals(that: Any): Boolean = that match
      case m: Money => amount == m.amount && currency == m.currency
      case _        => false
    override def hashCode: Int = (amount, currency).##
  println(Money(1, "EUR") == Money(1, "EUR"))
  println(Set(Money(1, "EUR"), Money(1, "EUR")).size)   // 1 — hashCode consistent
  // ⚠ subclassing + equals breaks symmetry (a == b but b != a) — prefer final classes
  //   or a `canEqual` method (which case classes generate).

  section("Classes are also types; `this` and self-references")
  class Node(val value: Int):
    var next: Node | Null = null
    def link(n: Node): Node = { next = n; this }
  val n = Node(1).link(Node(2))
  println(s"${n.value} -> ${n.next.nn.value}")

  section("Nested (inner) classes are path-dependent")
  class Outer:
    class Inner
    def make: Inner = Inner()
  val o1 = Outer()
  val o2 = Outer()
  val i1: o1.Inner = o1.make
  doesNotCompile("val i: o2.Inner = o1.make")    // o1.Inner ≠ o2.Inner
  val anyInner: Outer#Inner = i1                 // type projection = any Outer's Inner
  println(anyInner.getClass.getName)

  section("Value classes: AnyVal wrappers (no allocation, mostly)")
  println(Meters(3.0) + Meters(4.5))
  // ⚠ AnyVal classes are still boxed when used generically (List[Meters], Any, pattern match)
  // Scala 3 prefers opaque types for zero-cost wrappers (lessons/06-types).

  section("Constructor evaluation order")
  class Order:
    println("  1. body statement")
    val a = { println("  2. field a"); 1 }
    println("  3. another statement")
    def m = { println("  (method — not run at construction)"); 0 }
  Order()

class Meters(val value: Double) extends AnyVal:
  def +(o: Meters): Meters = Meters(value + o.value)
  override def toString = s"${value}m"

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
