//> using scala 3.9.0
// See also: lessons/02-functions/02_function_values.scala
// Run: scala-cli run lessons/02-functions/07_dependent_functions.scala
//
// Dependent function types: a function whose return type depends on the VALUE of its parameter.

package functions.dependent

import scala.compiletime.testing.typeCheckErrors

trait Entry:
  type Value
  def value: Value

// Path-dependent method
def extractValue(e: Entry): e.Value = e.value

// Dependent function type value: (e: Entry) => e.Value
// Under the hood, this extends Function1 with a refined, path-dependent apply method:
//   trait (e: Entry) => e.Value extends Function1[Entry, Any]:
//     def apply(e: Entry): e.Value
val extractor: (e: Entry) => e.Value = e => e.value

class IntEntry(v: Int) extends Entry:
  type Value = Int
  def value: Int = v

class StringEntry(v: String) extends Entry:
  type Value = String
  def value: String = v

@main def run(): Unit =
  section("Path-dependent method and dependent function value")
  val intEntry = IntEntry(42)
  val n: Int = extractor(intEntry)
  println(n) // 42

  val strEntry = StringEntry("scala 3")
  val s: String = extractor(strEntry)
  println(s) // scala 3

  section("Dependent function vs fixed return type")
  // `extractor` is NOT a plain `Entry => Any` or `Entry => Int`. When two entries have
  // different `Value` types, the return type tracks each specific argument value.
  val intResult: Int = extractor(intEntry)
  println(intResult) // 42

  section("Compile-time safety with typeCheckErrors")
  // Using the result of extracting from a StringEntry as an Int must not compile:
  doesNotCompile("""
    val e = StringEntry("oops")
    val bad: Int = extractor(e)
  """)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
