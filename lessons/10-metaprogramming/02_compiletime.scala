//> using scala 3.9.0
// Run: scala-cli run lessons/10-metaprogramming/02_compiletime.scala
//
// The scala.compiletime toolbox (all usable from inline code, no macros):
//   constValue / constValueOpt / constValueTuple — turn literal TYPES into values
//   erasedValue                                 — match on types in `inline match`
//   summonInline / summonFrom / summonAll        — implicit search at expansion time
//   error / codeOf / requireConst                — compile-time diagnostics
//   ops.int / ops.string / ops.boolean / ops.any — type-level arithmetic
//   testing.typeChecks / typeCheckErrors          — compile-time tests (used across lessons)

package meta.compiletime

import scala.compiletime.*
import scala.compiletime.ops.string.{+ as ++, Length}
import scala.compiletime.testing.{typeChecks, typeCheckErrors}

// constValueOpt: Some(value) for literal types, None otherwise
inline def maybeConst[T]: Option[Any] = constValueOpt[T]

// summonFrom: pick the first matching given
trait Show[A] { def show(a: A): String }
given Show[Int] = i => s"Int:$i"
inline def showOrToString[A](a: A): String = summonFrom {
  case s: Show[A] => s.show(a)
  case _          => s"(no Show) ${a.toString}"
}

// summonAll: a tuple of givens for a tuple of types
inline def showAllTypes[T <: Tuple](t: T): List[String] =
  val instances = summonAll[Tuple.Map[T, Show]].toList.asInstanceOf[List[Show[Any]]]
  t.toList.zip(instances).map((v, s) => s.show(v))

// requireConst: the argument must be a literal
inline def onlyLiterals(inline x: Int): Int =
  requireConst(x)
  x

// Type-level string concatenation (ops.string.+, renamed to ++ on import)
type Greeting = "Hello, " ++ "world"

// erasedValue-driven default values for any tuple of types
inline def defaults[T <: Tuple]: Tuple = inline erasedValue[T] match
  case _: EmptyTuple       => EmptyTuple
  case _: (Int *: ts)      => 0 *: defaults[ts]
  case _: (String *: ts)   => "" *: defaults[ts]
  case _: (Boolean *: ts)  => false *: defaults[ts]
  case _: (t *: ts)        => error("no default for a type in this tuple")

@main def run(): Unit =
  section("constValue family")
  println(constValue[42])
  println(constValue["lit"])
  println(maybeConst[7] -> maybeConst[Int])
  println(constValueTuple[(1, "two", true)])
  println(constValue[Greeting])
  println(constValue[Length["scala"]])

  section("summonFrom / summonInline / summonAll")
  println(showOrToString(5))
  println(showOrToString("no instance for String"))
  locally {
    given Show[String] = s => s"Str:$s"
    println(showAllTypes((1, "a")))
    println(showOrToString("now there is one"))
  }
  println(summonInline[Show[Int]].show(9))

  section("requireConst")
  println(onlyLiterals(3))
  println(typeCheckErrors("val n = 3; meta.compiletime.onlyLiterals(n)").map(_.message))

  section("erasedValue recursion over tuples")
  println(defaults[(Int, String, Boolean)])
  println(typeCheckErrors("meta.compiletime.defaults[(Int, Double)]").map(_.message))

  section("typeChecks: compile-time boolean")
  println(typeChecks("List(1).map(_ + 1)"))
  println(typeChecks("List(1).mapp(_ + 1)"))

  section("Where to go next")
  // When inline isn't enough (inspect arbitrary code, generate classes, read annotations),
  // use quoted macros: lessons/10-metaprogramming/macros/
  println("see macros/")

def section(title: String): Unit = println(s"\n=== $title ===")
