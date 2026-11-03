//> using scala 3.9.0
// Run: scala-cli run lessons/12-scala2/02_migration.scala
//
// Migrating Scala 2.13 code to Scala 3: a side-by-side reference, plus the tools.
//
// TOOLS
//   * Compile with `-source:3.0-migration` → old syntax becomes warnings, with `-rewrite`
//     the compiler fixes much of it automatically:
//       scala-cli compile . -O -source:3.0-migration -O -rewrite
//   * `-rewrite -indent` / `-rewrite -new-syntax` modernise syntax afterwards
//   * Scalafix rules (e.g. for implicits → givens) and the sbt-scala3-migrate plugin
//   * Cross-build: sbt `crossScalaVersions := Seq("2.13.16", "3.9.0")`, and
//     src/main/scala-2 / src/main/scala-3 folders for version-specific code
//
// COMPATIBILITY
//   * Scala 3 uses the Scala 2.13 standard library → Scala 3 can depend on 2.13 artifacts
//     (`.cross(CrossVersion.for3Use2_13)`), and 2.13 can depend on Scala 3 artifacts with
//     -Ytasty-reader (as long as they don't use Scala-3-only features in public APIs).
//   * Scala 2 MACROS don't run in Scala 3; libraries ship separate Scala 3 macro code.

package scala2.migration

import scala.compiletime.testing.typeCheckErrors

// Scala 2:  implicit class RichInt(val i: Int) extends AnyVal { def squared = i * i }
extension (i: Int) def squared: Int = i * i

// Scala 2:  trait Show[A] { def show(a: A): String }
//           implicit val intShow: Show[Int] = ...
//           def print[A](a: A)(implicit s: Show[A]) = s.show(a)
trait Show[A]:
  def show(a: A): String
given Show[Int] = a => s"Int($a)"
def render[A](a: A)(using s: Show[A]): String = s.show(a)

// Scala 2:  sealed trait Color; case object Red extends Color; ...
enum Color:
  case Red, Green

@main def run(): Unit =
  println("=== Things Scala 3 still accepts (for compatibility) ===")
  // implicit parameters/vals/defs still work in Scala 3 and interoperate with given/using:
  implicit val legacyConfig: String = "legacy"
  def needsString(using s: String): String = s
  println(needsString)                         // finds the implicit val
  println(render(42))
  println(7.squared)
  println(Color.values.toList)

  println("\n=== Things that changed meaning or were removed (compile errors in 3) ===")
  def err(code: String, errs: List[scala.compiletime.testing.Error]): Unit =
    println(s"  ${code.padTo(38, ' ')} → ${errs.headOption.map(_.message.linesIterator.next()).getOrElse("compiles (maybe with a warning)")}")
  err("def p() { }   // procedure syntax", typeCheckErrors("def p() { }"))
  err("'sym          // symbol literal", typeCheckErrors("val s = 'sym"))
  err("xs.toList _   // eta-expansion with _", typeCheckErrors("val f = List(1).map _"))
  err("forSome       // existential types", typeCheckErrors("def f(x: List[T] forSome { type T }) = 1"))
  err("do-while", typeCheckErrors("do { () } while (false)"))
  err("auto-application of ()-methods", typeCheckErrors("def m() = 1; val x: Int = m"))
  err("Int + String via any2stringadd?", typeCheckErrors("val o = new Object; o + \"x\""))

  println("\n=== Semantic changes worth knowing ===")
  // * Implicit resolution: different priority rules; implicit conversions need an import
  //   at the definition AND usage only applies with expected types.
  // * Inferred types of implicit/given definitions must be explicit for public members.
  // * Wildcard imports `import a._` don't import givens in Scala 3 (`import a.given`).
  // * Overloading resolution and type inference changed in corner cases (unions!).
  // * Case class companions no longer extend FunctionN (`Person.tupled` is gone).
  // * Scala 3 lazy vals are thread-safe with a different, deadlock-free encoding.
  // * Pattern-bound vals `val Some(x) = opt` warn about refutability.
  // * `private[this]` and `protected[this]` are deprecated (inferred automatically).
  println("see comments")

def section(title: String): Unit = println(s"\n=== $title ===")
