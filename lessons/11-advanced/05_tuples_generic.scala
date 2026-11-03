//> using scala 3.9.0
// Run: scala-cli run lessons/11-advanced/05_tuples_generic.scala
//
// Scala 3 tuples are heterogeneous lists (HLists): `(Int, String, Boolean)` is
// `Int *: String *: Boolean *: EmptyTuple`. Any arity, with list-like operations that keep
// precise types. (Scala 2 had Tuple1..Tuple22 only; Shapeless provided HLists.)

package advanced.tuples

import scala.compiletime.testing.typeCheckErrors

// Generic programming over tuples with a type class defined by induction
trait Csv[A]:
  def encode(a: A): List[String]
object Csv:
  given Csv[Int] = i => List(i.toString)
  given Csv[String] = s => List(s)
  given Csv[Boolean] = b => List(if b then "yes" else "no")
  given Csv[EmptyTuple] = _ => Nil
  given [H, T <: Tuple](using h: Csv[H], t: Csv[T]): Csv[H *: T] =
    tuple => h.encode(tuple.head) ++ t.encode(tuple.tail)

def toCsv[A](a: A)(using c: Csv[A]): String = c.encode(a).mkString(",")

case class P(name: String, age: Int)

@main def run(): Unit =
  section("Tuples are HLists")
  val t: (Int, String, Boolean) = (1, "a", true)
  val same: Int *: String *: Boolean *: EmptyTuple = t     // the same type, written as a list
  println(same == t)
  val consed = 0.5 *: t                                   // prepend: (Double, Int, String, Boolean)
  println(consed)
  val h *: rest = t                                       // pattern match on the cons
  println(s"head=$h rest=$rest")

  section("Typed operations")
  println(t.head)                  // Int
  println(t.tail)                  // (String, Boolean)
  println(t.last)
  println(t.init)
  println(t.take(2))               // (Int, String)
  println(t.drop(1))
  println(t.splitAt(1))
  println(t ++ (("x", 2.0)))       // concatenate (double parens: one tuple argument)
  println(t.reverse)               // (Boolean, String, Int) — typed!
  println(t.zip(("one", "two", "three")))
  println(t.toList)                // List[Int | String | Boolean]
  println(t.toArray.length)
  println(t.size)
  println(t(2))                    // index access, typed as Boolean
  val b: Boolean = t(2)            // the static type follows the index

  section("Polymorphic map over a tuple")
  val wrapped = t.map[List]([X] => (x: X) => List(x))
  println(wrapped)                 // (List(1), List(a), List(true))

  section("Big tuples (> 22)")
  val big = (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)
  println(big.size)
  println(big(23))
  println(big.getClass.getSimpleName)   // TupleXXL — backed by an array

  section("Generic derivation over tuples")
  println(toCsv((1, "Ada", true)))
  println(toCsv((42, (7, "nested"), false)))    // nested tuples: the same given recurses
  doesNotCompile("advanced.tuples.toCsv((1, 2.5))")   // no Csv[Double]

  section("Tuples <-> case classes")
  println(b)
  val m = summon[scala.deriving.Mirror.ProductOf[P]]
  println(m.fromProduct(("Bob", 3)))
  println(Tuple.fromProductTyped(P("Cy", 4)))

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
