//> using scala 3.9.0
//> using dep org.scala-lang::scala3-staging:3.9.0
// See also: lessons/10-metaprogramming/01_inline.scala
// See also: lessons/10-metaprogramming/macros/Main.scala
// Run: scala-cli run lessons/10-metaprogramming/04_staging.scala
//
// Multi-stage programming with scala.quoted.staging.
//
// Difference between staging and inlining:
//   - `inline` rewrites the call site at the program's own compile time.
//   - staging compiles a quote when the program runs, generating bytecode in-process.

package meta.staging

import scala.quoted.*
import scala.quoted.staging.*
import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("Compiling and running a quote at runtime")
  given Compiler = Compiler.make(Thread.currentThread().getContextClassLoader)

  // Staging compiles a quoted expression when the program runs:
  // adds one to 2
  val result: Int = staging.run {
    '{ 2 + 1 }
  }
  println(result) // 3

  section("Runtime code specialization")
  // Quotes can be composed and specialized dynamically before being passed to staging.run:
  def stagedPower(x: Expr[Double], n: Int)(using Quotes): Expr[Double] =
    if n == 0 then '{ 1.0 }
    else if n == 1 then x
    else '{ $x * ${stagedPower(x, n - 1)} }

  val cube = staging.run {
    '{ (x: Double) => ${stagedPower('{ x }, 3)} }
  }
  println(cube(2.0)) // 8.0

  section("Compile-time type checking with typeCheckErrors")
  // staging.run requires an expression block (Quotes ?=> Expr[T]), not raw values:
  doesNotCompile("scala.quoted.staging.run(42)")

  // ⚠ Quotes themselves ensure typed hygiene: type errors inside quotes are caught
  // at compile time (not faked). For example, '{ "not an int" : Int } produces:
  //   Found: ("not an int" : String)
  //   Required: Int
  println("quote typing safety checked by compiler (see comments)")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
