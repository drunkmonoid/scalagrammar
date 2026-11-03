//> using scala 3.9.0
// Run: scala-cli run lessons/03-oop/03_traits.scala
//
// Traits: interfaces with implementation, mixin composition, linearization,
// stackable modifications, trait parameters, self types, transparent traits.

package oop.traits

import scala.compiletime.testing.typeCheckErrors

// ---------- basics ----------
trait Shape:
  def area: Double                 // abstract
  def describe: String = f"${getClass.getSimpleName} with area $area%.2f"  // concrete

trait Named:
  val name: String                 // abstract val

class Square(side: Double) extends Shape, Named:   // Scala 3: comma-separated; `with` also works
  def area = side * side
  val name = "square"

// ---------- linearization ----------
// The linearization of a class C extends A with B with D is computed right-to-left:
//   C, then lin(D), then lin(B), then lin(A) — removing duplicates, keeping the LAST occurrence.
// `super` calls follow this order, NOT the declaration's parent.
trait Base:
  def hello: String = "Base"
trait A extends Base:
  override def hello: String = "A>" + super.hello
trait B extends Base:
  override def hello: String = "B>" + super.hello
trait C extends Base:
  override def hello: String = "C>" + super.hello
class X extends A, B, C            // linearization: X, C, B, A, Base, AnyRef, Any
class Y extends C, A, B            // Y, B, A, C, Base, ...

// ---------- stackable modifications ----------
abstract class IntQueue:
  def put(x: Int): Unit
  def get(): Int
class BasicIntQueue extends IntQueue:
  private val buf = collection.mutable.ArrayBuffer.empty[Int]
  def put(x: Int) = buf += x
  def get() = buf.remove(0)
trait Doubling extends IntQueue:
  abstract override def put(x: Int): Unit = super.put(2 * x)   // `abstract override`!
trait Incrementing extends IntQueue:
  abstract override def put(x: Int): Unit = super.put(x + 1)
trait Filtering extends IntQueue:
  abstract override def put(x: Int): Unit = if x >= 0 then super.put(x)

// ---------- trait parameters (Scala 3) ----------
trait Greeting(val greeting: String):
  def greet(who: String) = s"$greeting, $who!"
class EnglishGreeter extends Greeting("Hello")
// Only the first class in the hierarchy passes the arguments:
trait FormalGreeting extends Greeting:
  override def greet(who: String) = super.greet(s"dear $who")
class FormalEnglish extends EnglishGreeter, FormalGreeting

// ---------- self types: "requires", not "is-a" ----------
trait Logger:
  def log(msg: String): Unit = println(s"  [log] $msg")
trait Service:
  this: Logger =>                  // any concrete Service must ALSO be a Logger
  def work(): Unit = log("working")
class LoggedService extends Service, Logger

// ---------- transparent traits ----------
// Marker traits like Product/Serializable pollute inferred types. `transparent` hides them.
transparent trait Tagged
case object One extends Tagged
case object Two extends Tagged

// ---------- traits with conflicting members ----------
trait Left_  { def who = "left" }
trait Right_ { def who = "right" }
class Both extends Left_, Right_:
  override def who = super[Left_].who + "&" + super[Right_].who   // qualified super

@main def run(): Unit =
  section("Abstract + concrete members")
  val sq = Square(3)
  println(sq.describe)
  println(sq.name)

  section("Linearization decides super-call order")
  println(X().hello)   // C>B>A>Base
  println(Y().hello)   // B>A>C>Base
  println(classOf[X].getInterfaces.map(_.getSimpleName).toList)

  section("Stackable traits: order = right to left")
  val q1 = new BasicIntQueue with Incrementing with Filtering   // Filter first, then Increment
  q1.put(-1); q1.put(0); q1.put(1)
  println(List(q1.get(), q1.get()))   // List(1, 2)
  val q2 = new BasicIntQueue with Filtering with Incrementing   // Increment first, then Filter
  q2.put(-1); q2.put(0); q2.put(1)
  println(List(q2.get(), q2.get(), q2.get()))  // List(0, 1, 2)
  val q3 = new BasicIntQueue with Doubling with Incrementing
  q3.put(10)
  println(q3.get())    // (10+1)*2 = 22
  doesNotCompile("trait T extends IntQueue { override def put(x: Int) = super.put(x) }")

  section("Trait parameters")
  println(EnglishGreeter().greet("Ada"))
  println(FormalEnglish().greet("Ada"))
  doesNotCompile("class Bad extends EnglishGreeter, Greeting(\"Hi\")")  // passed twice
  doesNotCompile("class NoArgs extends Greeting")                        // never passed

  section("Self types")
  LoggedService().work()
  doesNotCompile("class Broken extends Service")

  section("Transparent traits keep inferred types clean")
  // The inferred element type is the join of One.type and Two.type. Transparent traits
  // (Tagged, and by default Product, Serializable, Comparable) are dropped from that join
  // where possible. Hover over `xs` in your IDE to see the inferred type.
  val xs = List(One, Two)
  println(xs)

  section("Qualified super")
  println(Both().who)

  section("Traits vs abstract classes")
  // Use an abstract class when: Java needs to extend it; you need constructor params in Scala 2.
  // Use a trait otherwise. A class can extend ONE class but many traits.
  doesNotCompile("class Z extends Square(1), BasicIntQueue")   // two classes

  section("Mixing traits into instances")
  val loud = new Logger { override def log(msg: String) = println(s"  [LOUD] ${msg.toUpperCase}") }
  loud.log("anonymous trait instance")

  section("Initialization order with abstract vals (classic trap)")
  trait HasSize:
    val size: Int
    val doubled: Int = size * 2           // ⚠ runs BEFORE the subclass initialises `size`
  class Ten extends HasSize:
    val size = 10
  println(Ten().doubled)                 // ⚠ 0, not 20
  // Fixes: make `doubled` a def or lazy val, or override with a constructor param:
  trait HasSize2:
    val size: Int
    lazy val doubled: Int = size * 2
  class Ten2 extends HasSize2:
    val size = 10
  println(Ten2().doubled)                // 20
  class Ten3(val size: Int = 10) extends HasSize   // class param initialised before trait body
  println(Ten3().doubled)                // 20

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
