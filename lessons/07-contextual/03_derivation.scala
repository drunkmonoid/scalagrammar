//> using scala 3.9.0
// Run: scala-cli run lessons/07-contextual/03_derivation.scala
//
// Type class derivation: `case class X(...) derives Show` generates a Show[X] instance
// at compile time from the shape of X, using scala.deriving.Mirror.
//
// A Mirror describes an ADT:
//   Mirror.ProductOf[T]  — case classes: MirroredElemTypes = field types, MirroredElemLabels = names
//   Mirror.SumOf[T]      — sealed traits/enums: MirroredElemTypes = the cases, ordinal(x)

package contextual.derivation

import scala.deriving.Mirror
import scala.compiletime.{constValue, erasedValue, summonFrom, constValueTuple}

// ---------- a type class we will derive ----------
trait Show[A]:
  def show(a: A): String

object Show:
  given Show[Int] = _.toString
  given Show[String] = s => s"\"$s\""
  given Show[Double] = _.toString
  given Show[Boolean] = _.toString
  given [A](using s: Show[A]): Show[List[A]] = _.map(s.show).mkString("[", ", ", "]")

  // Summon a Show for every type in a tuple of types, as a List
  inline def summonAll[T <: Tuple]: List[Show[?]] = inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => summonOrDerive[t] :: summonAll[ts]

  // Enum cases (Shape.Circle, ...) have no Show of their own: derive one on the fly.
  inline def summonOrDerive[T]: Show[T] = summonFrom {
    case s: Show[T]      => s
    case m: Mirror.Of[T] => derived[T](using m)
  }

  // `derived` is what `derives Show` calls
  inline def derived[A](using m: Mirror.Of[A]): Show[A] =
    lazy val instances = summonAll[m.MirroredElemTypes]       // lazy: supports recursive types
    val label = constValue[m.MirroredLabel]
    inline m match
      case p: Mirror.ProductOf[A] => showProduct(p, label, instances)
      case s: Mirror.SumOf[A]     => showSum(s, instances)

  private def showProduct[A](p: Mirror.ProductOf[A], label: String, instances: => List[Show[?]]): Show[A] =
    (a: A) =>
      val values = a.asInstanceOf[Product].productIterator.toList
      val names = a.asInstanceOf[Product].productElementNames.toList
      val parts = values.lazyZip(names).lazyZip(instances).map { (v, n, s) =>
        s"$n = ${s.asInstanceOf[Show[Any]].show(v)}"
      }
      parts.mkString(s"$label(", ", ", ")")

  private def showSum[A](s: Mirror.SumOf[A], instances: => List[Show[?]]): Show[A] =
    (a: A) => instances(s.ordinal(a)).asInstanceOf[Show[A]].show(a)

// ---------- deriving ----------
case class Address(street: String, number: Int) derives Show
case class User(name: String, age: Int, admin: Boolean, address: Address) derives Show

enum Shape derives Show:
  case Circle(radius: Double)
  case Rect(w: Double, h: Double)

// Recursive ADT works thanks to the lazy instances
enum Tree derives Show:
  case Leaf(value: Int)
  case Node(left: Tree, right: Tree)

// Standard library derivations: CanEqual (multiversal equality, see 11-advanced)
case class Token(s: String) derives CanEqual

// Deriving for a type you don't own: call `derived` explicitly
case class External(id: Int)
given Show[External] = Show.derived

@main def run(): Unit =
  section("Derived instances")
  val u = User("Ada", 36, true, Address("Main St", 1))
  println(summon[Show[User]].show(u))
  println(summon[Show[Shape]].show(Shape.Rect(2, 3)))
  println(summon[Show[Tree]].show(Tree.Node(Tree.Leaf(1), Tree.Node(Tree.Leaf(2), Tree.Leaf(3)))))
  println(summon[Show[List[Address]]].show(List(Address("A", 1), Address("B", 2))))
  println(summon[Show[External]].show(External(7)))

  section("Inspecting a Mirror directly")
  val m = summon[Mirror.ProductOf[User]]
  println(constValue[m.MirroredLabel])                 // "User"
  println(constValueTuple[m.MirroredElemLabels])       // (name,age,admin,address)
  val rebuilt: User = m.fromProduct(("Bob", 1, false, Address("x", 0)))
  println(rebuilt)
  val sm = summon[Mirror.SumOf[Shape]]
  println(sm.ordinal(Shape.Rect(1, 1)))                // 1
  println(constValueTuple[sm.MirroredElemLabels])      // (Circle,Rect)

  section("Tuples <-> case classes via Mirror")
  val tup = Tuple.fromProductTyped(Address("Elm", 5))  // (String, Int)
  println(tup)
  println(summon[Mirror.ProductOf[Address]].fromTuple(("Oak", 9)))

  section("What gets a Mirror?")
  // * case classes and case objects
  // * enums and enum cases
  // * sealed traits/classes whose children all have Mirrors
  // * NOT: plain classes, non-sealed traits
  println(scala.compiletime.testing.typeCheckErrors(
    "summon[scala.deriving.Mirror.Of[java.io.File]]").head.message.linesIterator.next())

  section("Libraries that derive for you")
  // circe:        case class X(...) derives Encoder.AsObject, Decoder
  // upickle:      derives ReadWriter
  // cats:         derives Eq, Show (via kittens)
  // zio-json:     derives JsonCodec
  // Magnolia:     generic derivation framework for Scala 2 & 3
  println("see ecosystem/")

def section(title: String): Unit = println(s"\n=== $title ===")
