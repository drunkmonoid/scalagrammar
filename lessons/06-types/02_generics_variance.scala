//> using scala 3.9.0
// Run: scala-cli run lessons/06-types/02_generics_variance.scala
//
// Generic classes and variance annotations:
//   Box[+A]  covariant      Cat <: Animal  ⇒  Box[Cat] <: Box[Animal]     (producers)
//   Box[-A]  contravariant  Cat <: Animal  ⇒  Box[Animal] <: Box[Cat]     (consumers)
//   Box[A]   invariant      no relationship                              (both / mutable)

package types.variance

import scala.compiletime.testing.typeCheckErrors

class Animal(val name: String):
  override def toString = s"${getClass.getSimpleName}($name)"
class Cat(name: String) extends Animal(name)
class Dog(name: String) extends Animal(name)

// ----- covariant: a read-only container -----
final case class Crate[+A](content: A):
  // ⚠ A covariant type can't appear in a method PARAMETER (contravariant position)…
  // def replace(a: A): Crate[A]                     // error
  // …so use a lower bound to widen instead:
  def replace[B >: A](b: B): Crate[B] = Crate(b)

// ----- contravariant: a consumer -----
trait Vet[-A]:
  def treat(a: A): String
val animalVet: Vet[Animal] = a => s"treating ${a.name}"

// ----- invariant: mutable cell -----
final class Cell[A](var value: A)

// ----- a covariant immutable stack (like List) -----
sealed trait Stack[+A]:
  def push[B >: A](b: B): Stack[B] = NonEmpty(b, this)
  def toList: List[A] = this match
    case NonEmpty(h, t) => h :: t.toList
    case Empty          => Nil
case object Empty extends Stack[Nothing]
final case class NonEmpty[+A](head: A, tail: Stack[A]) extends Stack[A]

@main def run(): Unit =
  section("Covariance")
  val catCrate: Crate[Cat] = Crate(Cat("Tom"))
  val animalCrate: Crate[Animal] = catCrate          // OK: Crate is covariant
  println(animalCrate)
  println(catCrate.replace(Dog("Rex")))               // widened to Crate[Animal]
  val cats: List[Cat] = List(Cat("a"))
  val animals: List[Animal] = cats                    // List is covariant
  println(animals)

  section("Contravariance")
  val catVet: Vet[Cat] = animalVet                    // a vet for any animal can treat cats
  println(catVet.treat(Cat("Tom")))
  doesNotCompile("val v: types.variance.Vet[types.variance.Animal] = (c: types.variance.Cat) => c.name")
  // Functions: Function1[-A, +B]
  val f: Animal => String = _.name
  val g: Cat => Any = f                               // wider input, narrower output
  println(g(Cat("x")))

  section("Invariance (and why mutable things must be invariant)")
  val catCell = Cell(Cat("Tom"))
  doesNotCompile("val ac: types.variance.Cell[types.variance.Animal] = types.variance.Cell(types.variance.Cat(\"t\")): types.variance.Cell[types.variance.Cat]")
  // If it compiled, we could do: ac.value = Dog("Rex") and then catCell.value would be a Dog.
  // Java arrays are covariant and fail at RUNTIME (ArrayStoreException). Scala arrays are invariant.
  println(catCell.value)

  section("Variance checking positions")
  doesNotCompile("class Bad[+A] { def put(a: A): Unit = () }")       // covariant in param position
  doesNotCompile("class Bad2[-A] { def get: A = ??? }")               // contravariant in result
  doesNotCompile("class Bad3[+A] { var x: A = ??? }")                 // var = getter + setter
  // private[this]-like (object-private) members are exempt:
  class Ok[+A](init: A):
    private var current: A = init                    // OK: not visible from outside
    def get: A = current
  println(Ok(1).get)

  section("Building a covariant collection")
  val s: Stack[Cat] = Empty.push(Cat("a")).push(Cat("b"))
  val s2: Stack[Animal] = s.push(Dog("d"))            // B inferred as Animal
  println(s2.toList)

  section("Nothing and covariance: one empty value for all types")
  val e1: Stack[Int] = Empty
  val e2: Stack[String] = Empty
  println(e1 == e2)

  section("Variance of standard types")
  //   List[+A], Vector[+A], Option[+A], Either[+L, +R], Future[+T], Map[K, +V], Set[A] (⚠ invariant!)
  //   Function1[-T, +R], Ordering[T] (invariant), Array[T] (invariant), mutable.* (invariant)
  val cs: Set[Cat] = Set(Cat("x"))
  doesNotCompile("val animalSet: Set[types.variance.Animal] = Set(types.variance.Cat(\"x\")): Set[types.variance.Cat]")
  val animalSet: Set[Animal] = cs.map(identity)              // explicit widen via map (or .toSet[Animal])
  println(animalSet)
  // Set is invariant because Set[A] extends (A => Boolean) — A appears in a parameter.

  section("Use-site variance with wildcards  ? <: / ? >:")
  def sizeOf(xs: Cell[? <: Animal]): String = xs.value.name         // read-only view
  println(sizeOf(Cell(Cat("Tom"))))
  def fill(c: Cell[? >: Cat]): Unit = c.value = Cat("New")          // write-only view
  val ac = Cell[Animal](Dog("d"))
  fill(ac)
  println(ac.value)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
