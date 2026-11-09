//> using scala 2.13.17
// Run: scala-cli run lessons/12-scala2/01_scala2_idioms.scala
//
// THIS FILE IS SCALA 2.13. Much production code (and Spark, and many libraries' sources)
// is written this way. Each section notes the Scala 3 equivalent.

package scala2.idioms

import scala.language.implicitConversions

// ---------- implicit parameters & values  (Scala 3: using / given) ----------
case class Config(name: String)

object Implicits {
  implicit val defaultConfig: Config = Config("scala2-app")
  def greet(user: String)(implicit cfg: Config): String = s"[${cfg.name}] hello $user"
}

// ---------- type classes with implicit objects/defs  (Scala 3: given ... with) ----------
trait Show[A] { def show(a: A): String }
object Show {
  def apply[A](implicit s: Show[A]): Show[A] = s                     // summoner
  implicit val intShow: Show[Int] = (a: Int) => s"Int($a)"
  implicit def listShow[A](implicit s: Show[A]): Show[List[A]] =     // derived instance
    (as: List[A]) => as.map(s.show).mkString("[", ", ", "]")
}

// ---------- implicit classes for syntax  (Scala 3: extension methods) ----------
object syntax {
  implicit class ShowOps[A](private val a: A) extends AnyVal {
    def show(implicit s: Show[A]): String = s.show(a)
  }
  implicit class RichString(private val s: String) extends AnyVal {
    def shout: String = s.toUpperCase + "!"
  }
}

// ---------- implicit conversions with implicit def  (Scala 3: given Conversion) ----------
case class Meters(v: Double)
object Meters {
  implicit def fromInt(i: Int): Meters = Meters(i.toDouble)
}

// ---------- sealed trait ADT  (Scala 3: enum) ----------
sealed trait Shape
final case class Circle(r: Double) extends Shape
final case class Square(s: Double) extends Shape
case object Dot extends Shape

// ---------- Enumeration (legacy; Scala 3: enum) ----------
object Weekday extends Enumeration {
  type Weekday = Value
  val Mon, Tue, Wed = Value
}

// ---------- existential types  (Scala 3: wildcards only) ----------
object Existentials {
  def size(xs: List[_]): Int = xs.size                              // `_` wildcard: works in 3 as `?`
  def sizeEx(xs: List[T] forSome { type T }): Int = xs.size          // forSome: dropped in Scala 3
}

// ---------- package object  (Scala 3: top-level definitions) ----------
// package object utils { def helper = ... }   — deprecated in Scala 3

// ---------- App trait with DelayedInit (Scala 3: @main) ----------
object Main extends App {
  import Implicits._
  import syntax._

  def section(t: String): Unit = println(s"\n=== $t ===")

  section("implicit parameters")
  println(greet("ada"))
  println(greet("bob")(Config("explicit")))      // pass explicitly WITHOUT `using`

  section("type classes + implicit class syntax")
  println(42.show)
  println(List(1, 2, 3).show)
  println(Show[List[Int]].show(List(9)))
  println(implicitly[Show[Int]].show(1))         // Scala 2's summon
  println("hey".shout)

  section("implicit conversion")
  def distance(m: Meters): String = s"${m.v} m"
  println(distance(5))

  section("pattern matching on a sealed trait")
  val shapes: List[Shape] = List(Circle(1), Square(2), Dot)
  shapes.foreach {
    case Circle(r) => println(s"  circle $r")
    case Square(s) => println(s"  square $s")
    case Dot       => println("  dot")
  }

  section("Enumeration")
  println(Weekday.values.toList)
  println(Weekday.withName("Tue").id)

  section("Syntax that is different / gone in Scala 3")
  // procedure syntax: def p() { ... }               → def p(): Unit = { ... }
  // symbol literals:  'sym                           → Symbol("sym")
  // vararg splices:   f(xs: _*)                      → f(xs*)
  // wildcard import:  import a._ / import a.{b => c} → import a.* / import a.{b as c}
  // `with` in types:  A with B                       → A & B
  // existential:      forSome                        → wildcards ?
  // implicit def/val/class                           → given / using / extension / Conversion
  // auto-application: calling `def f()` as `f`       → error for Scala-defined methods
  // do-while                                          → while ({ body; cond }) ()
  def proc(): Unit = { println("  (procedure syntax is deprecated even in 2.13)") }
  proc()
  def splice(xs: Int*): Int = xs.sum
  println(splice(List(1, 2, 3): _*))
  println(Existentials.size(List(1)) + Existentials.sizeEx(List("a")))
  println(Symbol("sym"))

  section("Scala 2 collections differences (2.12 vs 2.13)")
  // 2.12: CanBuildFrom, mapValues returning a view silently, Seq = collection.Seq, Stream
  // 2.13: Factory/BuildFrom, .view.mapValues, Seq = immutable.Seq, LazyList; 2.13 is
  //       binary-compatible with Scala 3's standard library (Scala 3 uses the 2.13 stdlib).
  println(LazyList.from(1).take(3).toList)

  section("Interop: Scala 3 can use Scala 2.13 libraries directly")
  // In sbt: libraryDependencies += ("org" %% "lib" % "v").cross(CrossVersion.for3Use2_13)
  // And Scala 2.13.x can read Scala 3 TASTy with -Ytasty-reader (limited to simple features).
  println("see 02_migration.scala")
}
