//> using scala 3.9.0
// See also: lessons/05-collections/01_hierarchy.scala
// Run: scala-cli run lessons/05-collections/08_factories.scala
//
// Collection Factories and BuildFrom: how Scala 2.13+/3 constructs collections uniformly.

package collections.factories

import scala.collection.{BuildFrom, Factory, IterableFactory}
import scala.collection.mutable.Builder
import scala.compiletime.testing.typeCheckErrors

case class MyBox[+A](elems: Vector[A])

object MyBox extends IterableFactory[MyBox]:
  def from[A](source: IterableOnce[A]): MyBox[A] =
    MyBox(Vector.from(source))
  def empty[A]: MyBox[A] =
    MyBox(Vector.empty)
  def newBuilder[A]: Builder[A, MyBox[A]] =
    Vector.newBuilder[A].mapResult(MyBox(_))

@main def run(): Unit =
  section("Array preservation: map produces an Array")
  val mappedArr = Array(1, 2, 3).map(_ + 1)
  println(mappedArr.getClass.isArray)              // true
  println(mappedArr.mkString("Array(", ", ", ")")) // Array(2, 3, 4)

  section("Converting with .to(Factory)")
  // .to takes a Factory[A, C]; companion objects like Vector implement Factory
  val vec = List(1, 2, 3).to(Vector)               // Vector companion implements Factory
  println(vec)                                     // Vector(1, 2, 3)

  section("Obtaining a Builder from a Factory")
  val factory: Factory[Int, Vector[Int]] = Vector
  val b: Builder[Int, Vector[Int]] = factory.newBuilder
  b += 10
  b += 20
  val built = b.result()
  println(built)                                   // Vector(10, 20)

  section("Unrelated conversions require copying")
  // ⚠ converting between unrelated collection types (e.g. Set to List) cannot reuse underlying storage; it copies every element
  val set = Set(1, 2, 3)
  val listFromSet = set.to(List)
  println(listFromSet)                             // List(1, 2, 3)

  section("Custom collection Factory")
  val custom: MyBox[Int] = List(1, 2, 3).to(MyBox)
  println(custom)                                  // MyBox(Vector(1, 2, 3))

  section("BuildFrom abstraction")
  def duplicate[From](coll: From)(using bf: BuildFrom[From, Int, From], ev: From <:< Iterable[Int]): From =
    val builder = bf.newBuilder(coll)
    for x <- ev(coll) do
      builder += x
      builder += x
    builder.result()

  val dupList: List[Int] = duplicate(List(1, 2))
  println(dupList)                                 // List(1, 1, 2, 2)

  section("Compile-time safety with typeCheckErrors")
  // .to(Map) requires elements to be pairs (Key, Value); calling it on List[Int] must fail:
  doesNotCompile("List(1, 2, 3).to(Map)")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
