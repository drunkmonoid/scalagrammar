//> using scala 3.9.0
// Run: scala-cli run lessons/07-contextual/02_type_classes.scala
//
// Type classes: ad-hoc polymorphism. Define behaviour for types you don't own,
// retroactively, without inheritance. The recipe:
//   1. a trait parameterised by the type:     trait Show[A]
//   2. instances as givens:                   given Show[Int] ...
//   3. an API using a context bound:          def print[A: Show](a: A)
//   4. (optional) extension syntax:           a.show

package contextual.typeclasses

import scala.compiletime.testing.typeCheckErrors

// ---------- 1. the type class ----------
trait Show[A]:
  extension (a: A) def show: String

object Show:
  def apply[A](using s: Show[A]): Show[A] = s     // "summoner": Show[Int].show(...)
  // Instances in the companion are found WITHOUT imports (implicit scope)
  given Show[Int] with
    extension (a: Int) def show = s"Int($a)"
  given Show[String] with
    extension (a: String) def show = s"\"$a\""
  // The extension is Show's single abstract method, so a lambda works (SAM conversion):
  given Show[Boolean] = b => if b then "yes" else "no"
  // Derived instances: Show for List[A] whenever Show[A] exists
  given [A: Show]: Show[List[A]] with
    extension (as: List[A]) def show = as.map(_.show).mkString("[", ", ", "]")
  given [A: Show, B: Show]: Show[(A, B)] with
    extension (p: (A, B)) def show = s"(${p._1.show}, ${p._2.show})"
  given [A: Show]: Show[Option[A]] with
    extension (o: Option[A]) def show = o.fold("none")(a => s"some(${a.show})")

// ---------- user type with its own instance ----------
case class Point(x: Int, y: Int)
object Point:
  given Show[Point] with
    // ⚠ Inside an extension method named `show`, `p.x.show` would resolve to THIS method
    //   (the enclosing definition shadows other `show` extensions) and fail to type check.
    //   Call the Int instance explicitly instead:
    extension (p: Point) def show = s"Point(${Show[Int].show(p.x)}, ${Show[Int].show(p.y)})"

// ---------- 3. generic API ----------
def describe[A: Show](a: A): String = s"Value: ${a.show}"
def showAll[A: Show](as: A*): String = as.map(_.show).mkString(" | ")

// ---------- Monoid: a type class with laws ----------
trait Semigroup[A]:
  extension (x: A) def |+|(y: A): A
trait Monoid[A] extends Semigroup[A]:
  def empty: A
object Monoid:
  def apply[A](using m: Monoid[A]) = m
  given Monoid[Int] with
    def empty = 0
    extension (x: Int) def |+|(y: Int) = x + y
  given Monoid[String] with
    def empty = ""
    extension (x: String) def |+|(y: String) = x + y
  given [A]: Monoid[List[A]] with
    def empty = Nil
    extension (x: List[A]) def |+|(y: List[A]) = x ++ y
  given [K, V: Monoid]: Monoid[Map[K, V]] with
    def empty = Map.empty
    extension (x: Map[K, V]) def |+|(y: Map[K, V]) =
      y.foldLeft(x) { case (acc, (k, v)) => acc.updated(k, acc.get(k).fold(v)(_ |+| v)) }

def combineAll[A: Monoid](as: List[A]): A = as.foldLeft(Monoid[A].empty)(_ |+| _)

// ---------- multiple instances for one type: newtype or explicit passing ----------
object ProductMonoid:
  given Monoid[Int] with
    def empty = 1
    extension (x: Int) def |+|(y: Int) = x * y

// ---------- type class with a type-level output: "functional dependency" ----------
trait Container[F]:
  type Elem
  def first(f: F): Elem
given Container[String] with
  type Elem = Char
  def first(s: String) = s.head
def firstOf[F](f: F)(using c: Container[F]): c.Elem = c.first(f)

@main def run(): Unit =
  section("Using instances")
  // ⚠ Two different lookups are involved:
  //   * describe(42) needs a Show[Int] VALUE → searched in the implicit scope of Show[Int],
  //     which includes object Show. No import needed.
  //   * 42.show needs an EXTENSION METHOD on Int → searched in lexical scope and in the
  //     implicit scope of Int (which doesn't include Show). So we import the instances:
  import Show.given
  println(42.show)
  println("hi".show)
  println(true.show)
  println(List(1, 2, 3).show)
  println((1, "a").show)
  println(List(Option(1), None).show)
  println(Point(1, 2).show)
  println(describe(List(Point(0, 0))))
  println(showAll(1, 2, 3))
  println(Show[Int].show(7))                 // summoner + extension called as a plain method
  println(Point(3, 4).show)                  // found via Point's companion (implicit scope of Point)

  section("Missing instances are compile errors")
  doesNotCompile("contextual.typeclasses.describe(3.14)")
  doesNotCompile("contextual.typeclasses.describe(List(3.14))")  // no Show[Double] ⇒ no Show[List[Double]]

  section("Monoids")
  println(combineAll(List(1, 2, 3, 4)))
  println(combineAll(List("a", "b", "c")))
  println(combineAll(List(List(1), List(2, 3))))
  println(combineAll(List(Map("a" -> 1, "b" -> 2), Map("a" -> 10))))   // merges with Int monoid
  println(combineAll(List.empty[Int]))       // empty → identity

  section("Overriding the default instance locally")
  locally {
    import ProductMonoid.given                // lexical scope beats implicit (companion) scope
    println(combineAll(List(1, 2, 3, 4)))     // 24
  }
  println(combineAll(List(1, 2, 3, 4))(using ProductMonoid.given_Monoid_Int))   // pass explicitly

  section("Type-level outputs")
  val c: Char = firstOf("scala")
  println(c)

  section("Laws (check with property tests — see ecosystem/testing)")
  // associativity:   (a |+| b) |+| c == a |+| (b |+| c)
  // identity:        empty |+| a == a == a |+| empty
  locally {
    import Monoid.given                      // |+| on Int needs the instance in lexical scope
    val (a, b, c) = (1, 2, 3)
    println(((a |+| b) |+| c) == (a |+| (b |+| c)))
    println((Monoid[Int].empty |+| a) == a)
  }

  section("Anatomy of the generated names")
  // Anonymous givens get synthesised names: given_Show_Int, given_Monoid_List, ...
  println(Show.given_Show_Int.getClass.getSimpleName)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
