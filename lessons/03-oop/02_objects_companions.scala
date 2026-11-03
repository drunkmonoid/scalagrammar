//> using scala 3.9.0
// Run: scala-cli run lessons/03-oop/02_objects_companions.scala
//
// `object` = a singleton, lazily initialised on first access. Used for: utilities,
// companions (static-like members, factories), modules, and case objects.

package oop.objects

object MathUtils:
  println("  [MathUtils initialised]")   // runs once, on first access
  val Tau: Double = 2 * math.Pi
  def square(x: Double): Double = x * x

// Companion: same name, same file. Class and companion can see each other's privates.
class Account private (val id: Int, private var balance: BigDecimal):
  def deposit(amount: BigDecimal): Unit =
    require(amount > 0)
    balance += amount
    Account.totalDeposits += amount   // accessing companion's private member
  override def toString = s"Account#$id($balance)"

object Account:
  private var nextId = 1
  private var totalDeposits: BigDecimal = 0
  def apply(initial: BigDecimal = 0): Account =   // factory: Account(100)
    val a = new Account(nextId, initial)
    nextId += 1
    a
  def unapply(a: Account): Some[(Int, BigDecimal)] = Some((a.id, a.balance))  // reads private balance
  def stats: String = s"accounts=${nextId - 1} deposits=$totalDeposits"

// Objects can extend classes and traits
trait Greeter:
  def greet(name: String): String
object English extends Greeter:
  def greet(name: String) = s"Hello, $name"
object French extends Greeter:
  def greet(name: String) = s"Bonjour, $name"

// Companion objects are also where implicit/given instances live (implicit scope):
case class Celsius(value: Double)
object Celsius:
  given Ordering[Celsius] = Ordering.by(_.value)
  val Freezing = Celsius(0)

@main def run(): Unit =
  section("Lazy initialisation of objects")
  println("before first access")
  println(MathUtils.Tau)
  println(MathUtils.square(3))       // no second init message

  section("Companion factory & privates")
  val a = Account(100)
  a.deposit(50)
  val b = Account()
  b.deposit(1)
  println(a); println(b)
  println(Account.stats)
  a match
    case Account(id, bal) => println(s"extracted id=$id balance=$bal")

  section("Objects as values (they have a singleton type)")
  val greeters: List[Greeter] = List(English, French)
  greeters.foreach(g => println(g.greet("Ada")))
  val e: English.type = English      // the singleton type
  println(e eq English)

  section("Implicit scope: the companion is searched automatically")
  println(List(Celsius(30), Celsius(-5), Celsius.Freezing).sorted)   // no import needed

  section("Case objects")
  // A case object gets toString, hashCode, serialization (readResolve) and pattern matching
  println(Status.Active)
  println(Status.values.mkString(", "))

  section("Static forwarders & Java interop")
  // Top-level object methods get static forwarders in the companion class, so Java can call
  // MathUtils.square(3.0) directly. Use @static (scala.annotation.static) to emit a TRUE
  // static field/method in the companion class:
  println(Registry.count)

  section("Objects inside classes are per-instance")
  class Box:
    object Meta:
      val created = System.nanoTime()
  val b1 = Box()
  val b2 = Box()
  println(b1.Meta eq b2.Meta)        // false — one object per Box instance

  section("Top-level definitions (Scala 3) replace package objects")
  println(topLevelHelper(21))        // defined below, outside any object

  section("Initialisation order trap between objects")
  // ⚠ Cyclic object initialisation yields default values (null/0), not an error:
  println(s"A.x = ${A.x}, B.y = ${B.y}")   // one of them sees 0 — see definitions below

object Status:
  sealed trait S
  case object Active extends S
  case object Disabled extends S
  def values: List[S] = List(Active, Disabled)

class Registry
object Registry:
  @scala.annotation.static val count: Int = 3   // a real JVM static on class Registry

def topLevelHelper(x: Int): Int = x * 2

object A:
  val x: Int = B.y + 1
object B:
  val y: Int = A.x + 1

def section(title: String): Unit = println(s"\n=== $title ===")
