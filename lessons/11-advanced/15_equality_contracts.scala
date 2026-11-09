//> using scala 3.9.0
// See also: lessons/03-oop/01_classes.scala
// See also: lessons/11-advanced/02_multiversal_equality.scala
// Run: scala-cli run lessons/11-advanced/15_equality_contracts.scala
//
// Equality, Ordering, and HashCode contracts in Scala:
// 1. Subclassing and the canEqual pattern for symmetric equals
// 2. Ordering inconsistent with equals causing dropped elements in TreeSet
// 3. Scala BigDecimal vs java.math.BigDecimal equals & hashCode semantics
// 4. Any.## versus Object.hashCode across numeric types
// 5. Mutable keys in hash collections

package advanced.equalitycontracts

import scala.collection.immutable.TreeSet

// --- Section 1: Naive (broken) hierarchy ---
class NaivePoint(val x: Int, val y: Int):
  override def equals(other: Any): Boolean = other match
    case that: NaivePoint => this.x == that.x && this.y == that.y
    case _                => false
  override def hashCode(): Int = (x, y).##

class NaiveColorPoint(x: Int, y: Int, val color: String) extends NaivePoint(x, y):
  override def equals(other: Any): Boolean = other match
    case that: NaiveColorPoint => this.x == that.x && this.y == that.y && this.color == that.color
    case _                     => false
  override def hashCode(): Int = (x, y, color).##

// --- Section 1: Symmetric hierarchy with canEqual ---
class SafePoint(val x: Int, val y: Int):
  def canEqual(other: Any): Boolean = other.isInstanceOf[SafePoint]

  override def equals(other: Any): Boolean = other match
    case that: SafePoint => that.canEqual(this) && this.x == that.x && this.y == that.y
    case _               => false

  override def hashCode(): Int = (x, y).##

class SafeColorPoint(x: Int, y: Int, val color: String) extends SafePoint(x, y):
  override def canEqual(other: Any): Boolean = other.isInstanceOf[SafeColorPoint]

  override def equals(other: Any): Boolean = other match
    case that: SafeColorPoint => that.canEqual(this) && this.x == that.x && this.y == that.y && this.color == that.color
    case _                    => false

  override def hashCode(): Int = (x, y, color).##

@main def run(): Unit =
  section("1. Broken symmetry without canEqual vs fixed symmetry with canEqual")
  val np = NaivePoint(1, 2)
  val ncp = NaiveColorPoint(1, 2, "crimson")
  println(s"Naive before canEqual:")
  println(s"  np == ncp:  ${np == ncp}")  // true!
  println(s"  ncp == np:  ${ncp == np}")  // false! (Symmetry violation: a == b != b == a)
  assert((np == ncp) != (ncp == np))

  val sp = SafePoint(1, 2)
  val scp = SafeColorPoint(1, 2, "crimson")
  println(s"Safe with canEqual:")
  println(s"  sp == scp:  ${sp == scp}")  // false
  println(s"  scp == sp:  ${scp == sp}")  // false (Symmetry restored!)
  assert((sp == scp) == (scp == sp))

  section("2. Ordering inconsistent with equals in TreeSet")
  // Ordering strings strictly by length:
  given lengthOrdering: Ordering[String] = Ordering.by(_.length)
  // "cat" and "dog" have equal length 3.
  // Because compare("cat", "dog") == 0, TreeSet treats "dog" as a duplicate key and drops it!
  val words = TreeSet("cat", "dog", "elephant")(using lengthOrdering)
  println(s"TreeSet ordered by string length: $words") // TreeSet(cat, elephant)
  println(s"Words size: ${words.size}")                // 2
  println(s"Contains 'dog': ${words.contains("dog")}") // true (finds 'cat' at length 3!)
  assert(words.size == 2)
  assert(!words.exists(_ == "dog"))

  section("3. Scala BigDecimal vs java.math.BigDecimal")
  val s1 = BigDecimal("1.0")
  val s2 = BigDecimal("1.00")
  println(s"Scala BigDecimal:")
  println(s"  s1 == s2:               ${s1 == s2}")                     // true
  println(s"  s1.hashCode == s2.hash: ${s1.hashCode == s2.hashCode}")   // true
  assert(s1 == s2)
  assert(s1.hashCode == s2.hashCode)

  val j1 = new java.math.BigDecimal("1.0")
  val j2 = new java.math.BigDecimal("1.00")
  println(s"Java BigDecimal:")
  println(s"  j1.equals(j2):          ${j1.equals(j2)}")                // false (different scales: 1 vs 2)
  println(s"  j1.compareTo(j2) == 0:  ${j1.compareTo(j2) == 0}")        // true (numerical value is equal)
  println(s"  j1.hashCode == j2.hash: ${j1.hashCode() == j2.hashCode()}") // false
  assert(!j1.equals(j2))
  assert(j1.hashCode() != j2.hashCode())

  section("4. ## versus hashCode for numeric equality")
  // In Java, Integer(1).hashCode() != Double(1.0).hashCode()
  val intVal: Int = 1
  val doubleVal: Double = 1.0
  val longVal: Long = 1L
  val floatVal: Float = 1.0f

  println(s"Standard Java hashCode:")
  println(s"  intVal.hashCode:    ${intVal.hashCode}")
  println(s"  doubleVal.hashCode: ${doubleVal.hashCode}")
  println(s"  hashCode equal:     ${intVal.hashCode == doubleVal.hashCode}") // false!

  println(s"Scala ## universal hashing:")
  println(s"  intVal.##:    ${intVal.##}")
  println(s"  doubleVal.##: ${doubleVal.##}")
  println(s"  longVal.##:   ${longVal.##}")
  println(s"  floatVal.##:  ${floatVal.##}")
  assert(intVal.## == doubleVal.##)
  assert(intVal.## == longVal.##)
  assert(intVal.## == floatVal.##)

  section("5. Mutable key inside HashMap")
  println("See lessons/05-collections/07_performance_pitfalls.scala for a full demonstration")
  println("of how mutating an object used as a HashSet/HashMap key corrupts bucket lookup.")

def section(title: String): Unit = println(s"\n=== $title ===")
