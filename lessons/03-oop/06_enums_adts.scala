//> using scala 3.9.0
// Run: scala-cli run lessons/03-oop/06_enums_adts.scala
//
// Algebraic Data Types (ADTs): sum types (OR) and product types (AND).
// Scala 3 `enum` covers simple enumerations, parameterised enums, ADTs and GADTs.

package oop.enums

import scala.compiletime.testing.typeCheckErrors

// ---------- simple enumeration ----------
enum Color:
  case Red, Green, Blue

// ---------- enum with parameters and members ----------
enum Planet(val mass: Double, val radius: Double):
  case Mercury extends Planet(3.303e+23, 2.4397e6)
  case Earth   extends Planet(5.976e+24, 6.37814e6)
  case Jupiter extends Planet(1.9e+27, 7.1492e7)

  private final val G = 6.67300e-11
  def surfaceGravity: Double = G * mass / (radius * radius)
  def surfaceWeight(otherMass: Double): Double = otherMass * surfaceGravity

object Planet:                            // companion: add helpers
  def heaviest: Planet = values.maxBy(_.mass)

// ---------- ADT ----------
enum Shape:
  case Circle(radius: Double)
  case Rectangle(width: Double, height: Double)
  case Point                               // a singleton case
  def area: Double = this match
    case Circle(r)       => math.Pi * r * r
    case Rectangle(w, h) => w * h
    case Point           => 0

// ---------- generic ADT (covariant) ----------
enum MyOption[+A]:
  case MySome(value: A)
  case MyNone                              // extends MyOption[Nothing] automatically
  def getOrElse[B >: A](default: => B): B = this match
    case MySome(v) => v
    case MyNone    => default

// ---------- GADT: type refinement in pattern matching ----------
enum Expr[A]:
  case IntLit(i: Int)                    extends Expr[Int]
  case BoolLit(b: Boolean)               extends Expr[Boolean]
  case Add(l: Expr[Int], r: Expr[Int])   extends Expr[Int]
  case If(c: Expr[Boolean], t: Expr[A], e: Expr[A])
  case Eq(l: Expr[Int], r: Expr[Int])    extends Expr[Boolean]

def eval[A](e: Expr[A]): A = e match
  case Expr.IntLit(i)   => i            // here the compiler knows A = Int
  case Expr.BoolLit(b)  => b            // and here A = Boolean
  case Expr.Add(l, r)   => eval(l) + eval(r)
  case Expr.If(c, t, f) => if eval(c) then eval(t) else eval(f)
  case Expr.Eq(l, r)    => eval(l) == eval(r)

// ---------- the Scala 2 encoding (still common) ----------
sealed trait Json
object Json:
  case object JNull extends Json
  final case class JNum(n: Double) extends Json
  final case class JStr(s: String) extends Json
  final case class JArr(items: List[Json]) extends Json
  final case class JObj(fields: Map[String, Json]) extends Json

def render(j: Json): String = j match
  case Json.JNull       => "null"
  case Json.JNum(n)     => n.toString
  case Json.JStr(s)     => "\"" + s + "\""
  case Json.JArr(items) => items.map(render).mkString("[", ",", "]")
  case Json.JObj(fs)    => fs.map((k, v) => s"\"$k\":${render(v)}").mkString("{", ",", "}")

// ---------- enums interoperating with Java ----------
enum JColor extends java.lang.Enum[JColor]:
  case Cyan, Magenta

// ---------- recursive ADT ----------
enum LinkedList[+A]:
  case Cons(head: A, tail: LinkedList[A])
  case Empty
  def toList: List[A] = this match
    case Cons(h, t) => h :: t.toList
    case Empty      => Nil

@main def run(): Unit =
  section("Simple enums")
  println(Color.Red)
  println(Color.values.toList)           // Array[Color] → List
  println(Color.valueOf("Green"))
  println(Color.Blue.ordinal)            // 2
  println(Color.fromOrdinal(0))
  try Color.valueOf("Purple")
  catch case e: IllegalArgumentException => println(s"valueOf unknown → ${e.getMessage}")
  // Enum values are singletons: == and eq agree
  println(Color.Red eq Color.valueOf("Red"))

  section("Parameterised enums")
  val earthWeight = 75.0
  val mass = earthWeight / Planet.Earth.surfaceGravity
  for p <- Planet.values do println(f"  ${p}%-8s weight: ${p.surfaceWeight(mass)}%6.2f")
  println(Planet.heaviest)

  section("ADT enums")
  val shapes = List(Shape.Circle(1), Shape.Rectangle(2, 3), Shape.Point)
  shapes.foreach(s => println(f"  $s%-24s area=${s.area}%.2f"))
  // ⚠ The apply of an enum case returns the ENUM type (widened), not the case type:
  val c = Shape.Circle(2)                // c: Shape (not Shape.Circle)
  doesNotCompile("Shape.Circle(2).radius")
  val c2 = new Shape.Circle(2)           // `new` gives the precise case type
  println(c2.radius)
  println(c.ordinal)                     // ADT cases have ordinals too
  doesNotCompile("Shape.values")         // values only exists if ALL cases are singletons

  section("Generic enum & variance")
  val opts: List[MyOption[Int]] = List(MyOption.MySome(1), MyOption.MyNone)
  println(opts.map(_.getOrElse(-1)))

  section("GADT evaluation")
  import Expr.*
  val program = If(Eq(Add(IntLit(1), IntLit(2)), IntLit(3)), IntLit(100), IntLit(0))
  println(eval(program))                 // 100 — and its static type is Int
  doesNotCompile("Expr.Add(Expr.IntLit(1), Expr.BoolLit(true))")

  section("Sealed trait encoding")
  println(render(Json.JObj(Map("a" -> Json.JNum(1), "b" -> Json.JArr(List(Json.JNull, Json.JStr("x")))))))

  section("Java-compatible enum")
  println(JColor.Cyan.compareTo(JColor.Magenta))
  println(java.util.EnumSet.allOf(classOf[JColor]))

  section("Recursive ADT")
  println(LinkedList.Cons(1, LinkedList.Cons(2, LinkedList.Empty)).toList)

  section("Exhaustiveness")
  def name(c: Color): String = c match
    case Color.Red   => "red"
    case Color.Green => "green"
    case Color.Blue  => "blue"
  println(name(Color.Green))
  // Dropping a case gives: "match may not be exhaustive. It would fail on pattern case: Blue"

  section("Enum cases are values of their companion")
  // Under the hood: `enum Color` → sealed abstract class Color + companion with vals.
  println(Color.getClass.getName)
  println(Color.Red.getClass.getName)
  println(Shape.Circle(1).getClass.getName)

  section("Product vs sum cardinality (why 'algebraic')")
  // |Boolean| = 2,  |Color| = 3
  // product (Boolean, Color) has 2 × 3 = 6 inhabitants
  // sum Either[Boolean, Color] has 2 + 3 = 5 inhabitants
  val products = for b <- List(true, false); c <- Color.values.toList yield (b, c)
  val sums = List(true, false).map(Left(_)) ++ Color.values.map(Right(_))
  println(s"product: ${products.size}, sum: ${sums.size}")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
