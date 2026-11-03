//> using scala 3.9.0
// Run: scala-cli run lessons/06-types/07_type_lambdas_match_types.scala
//
// Type-level programming in Scala 3:
//   * type lambdas         [X] =>> F[X, Int]
//   * match types          type Elem[X] = X match { case List[t] => t ... }
//   * recursive match types and compile-time arithmetic (scala.compiletime.ops)
//   * tuples as type-level lists

package types.typelevel

import scala.compiletime.ops.int.*
import scala.compiletime.{constValue, erasedValue}
import scala.compiletime.testing.typeCheckErrors

// ---------- type lambdas ----------
type MapTo[V] = [K] =>> Map[K, V]          // a type lambda returned by an alias
type IntMap = MapTo[Int]                    // [K] =>> Map[K, Int]
val im: IntMap[String] = Map("a" -> 1)
type Flip[F[_, _]] = [A, B] =>> F[B, A]
val flipped: Flip[Either][Int, String] = Left("left is now String")

// ---------- match types ----------
type Elem[X] = X match
  case String      => Char
  case Array[t]    => t
  case Iterable[t] => t
  case Option[t]   => t

def firstElem[X](x: X): Elem[X] = x match
  case s: String      => s.charAt(0)
  case a: Array[t]    => a(0)
  case i: Iterable[t] => i.head
  case o: Option[t]   => o.get

// Recursive match type: flatten nested containers
type LeafElem[X] = X match
  case String      => Char
  case Iterable[t] => LeafElem[t]
  case Array[t]    => LeafElem[t]
  case AnyVal      => X

// Type-level conditional
type IsString[X] <: Boolean = X match
  case String => true
  case _      => false

// ---------- compile-time arithmetic ----------
type Three = 1 + 2
val three: Three = 3                        // the type IS the literal 3
type Fact[N <: Int] <: Int = N match
  case 0 => 1
  case _ => N * Fact[N - 1]
val f5: Fact[5] = 120

// ---------- tuples as type-level lists ----------
type Tup = (Int, String, Boolean)
type Second = Tuple.Elem[Tup, 1]            // String
type Size = Tuple.Size[Tup]                 // 3
type Mapped = Tuple.Map[Tup, Option]        // (Option[Int], Option[String], Option[Boolean])
type Concat = Tuple.Concat[(Int, Int), (String, Boolean)]

// Recursive match type over a tuple: our own version of Tuple.Size
// (the `<: Int` bound on the result is what lets us use `+` on the recursive call)
type Len[T <: Tuple] <: Int = T match
  case EmptyTuple => 0
  case _ *: tail  => 1 + Len[tail]

inline def sizeOf[T <: Tuple]: Int = constValue[Tuple.Size[T]]

// Type-level string description
type Describe[X] <: String = X match
  case Int     => "an int"
  case String  => "a string"
  case List[t] => "a list"
  case _       => "something"
inline def describe[X]: String = constValue[Describe[X]]

inline def defaultOf[T]: Any = inline erasedValue[T] match
  case _: Int     => 0
  case _: String  => ""
  case _: Boolean => false
  case _          => null

@main def run(): Unit =
  section("Type lambdas")
  println(im)
  println(flipped)

  section("Match types")
  val c: Char = firstElem("hello")         // Elem[String] reduces to Char
  val i: Int = firstElem(List(1, 2))       // Elem[List[Int]] reduces to Int
  val d: Double = firstElem(Array(2.5))
  val o: String = firstElem(Option("x"))
  println(s"$c $i $d $o")
  summon[Elem[List[Int]] =:= Int]
  summon[LeafElem[List[Vector[Array[Int]]]] =:= Int]
  summon[LeafElem[List[String]] =:= Char]
  summon[IsString[String] =:= true]
  summon[IsString[Int] =:= false]
  // ⚠ a match type that can't be reduced stays abstract → errors at use:
  doesNotCompile("val x: types.typelevel.Elem[Int] = 1")
  println("match-type proofs passed")

  section("Compile-time arithmetic")
  println(three -> f5)
  println(constValue[Fact[6]])             // 720 computed by the TYPE CHECKER
  doesNotCompile("val wrong: types.typelevel.Fact[3] = 7")
  println(constValue[3 * 4 + 1])
  println(constValue[ToString[42]])        // "42" (ops.any / ops.int ToString)
  println(constValue[Max[3, 9]])

  section("Tuples at the type level")
  summon[Second =:= String]
  summon[Mapped =:= (Option[Int], Option[String], Option[Boolean])]
  summon[Concat =:= (Int, Int, String, Boolean)]
  println(sizeOf[Tup])
  println(constValue[Len[(Int, String, Boolean, Char)]])   // 4
  // Value-level tuple ops mirror them:
  val t: Tup = (1, "two", true)
  println(t(1))                            // "two", typed as String
  println(t.size)
  println(t.map[Option]([X] => (x: X) => Option(x)))
  println(t ++ ((3.0, 'c')))               // double parens: `t ++ (a, b)` would be 2 arguments
  println(t.toList)                        // List[Int | String | Boolean]
  println(t.drop(1))

  section("Type-level strings")
  println(describe[Int])
  println(describe[List[String]])
  println(describe[Double])

  section("erasedValue: pattern match on TYPES in inline code")
  println(List(defaultOf[Int], defaultOf[String], defaultOf[Boolean], defaultOf[Double]))

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
