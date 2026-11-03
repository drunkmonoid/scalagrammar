//> using scala 3.9.0
//> using mainClass basics.hello.greet
// Run:  scala-cli run lessons/01-basics/01_hello.scala -- Ada 3
// Other entry points in this file:
//       scala-cli run lessons/01-basics/01_hello.scala --main-class basics.hello.serve -- 8080
//       scala-cli run lessons/01-basics/01_hello.scala --main-class basics.hello.JavaStyle
//
// ENTRY POINTS
// ------------
// Scala 3 has three ways to define a program entry point:
//   1. @main methods (idiomatic Scala 3)
//   2. an object with `def main(args: Array[String]): Unit` (Java-style, works everywhere)
//   3. `object X extends App` (Scala 2 style; still works but has init-order pitfalls, see below)
//
// A file can contain several @main methods; scala-cli asks which one to run
// (or pass `--main-class basics.hello.greet`).

package basics.hello

// (1) @main with typed parameters.
// The compiler generates a class named `greet` with a real `main(Array[String])`
// that parses each argument using a `scala.util.CommandLineParser.FromString[T]` instance.
// Missing / unparsable args produce an error message instead of a stack trace.
// Default values make arguments optional (so this file also runs with no args).
@main def greet(name: String = "world", times: Int = 2): Unit =
  for i <- 1 to times do println(s"[$i] Hello, $name!")

  section("Program arguments")
  // Varargs: `@main def run(args: String*)` collects everything.
  println("`@main def f(xs: Int*)` collects all remaining args as a Seq[Int]")

  section("What did the compiler generate?")
  // The generated wrapper class has the same name as the method, in this package.
  val cls = Class.forName("basics.hello.greet")
  println(s"generated class: ${cls.getName}, has main: ${cls.getMethods.exists(_.getName == "main")}")

  section("Process / environment")
  println(s"java.version      = ${sys.props("java.version")}")
  println(s"scala version     = ${scala.util.Properties.versionNumberString}")
  println(s"HOME env var      = ${sys.env.getOrElse("HOME", "<unset>")}")
  println(s"available cores   = ${Runtime.getRuntime.availableProcessors}")

  section("Exit codes")
  // sys.exit(n) terminates the JVM. Its return type is Nothing, so it fits anywhere:
  def check(ok: Boolean): Int = if ok then 42 else sys.exit(1)
  println(s"check(true) = ${check(true)}")

  section("Main via object (Java style)")
  JavaStyle.main(Array("from greet"))

  section("App trait pitfall")
  // Scala 2's `extends App` relied on DelayedInit, which Scala 3 dropped. In Scala 3 the
  // body is just the object's initializer: it runs on FIRST ACCESS to the object, not when
  // main is called. Consequences:
  //   * merely touching OldStyle.x below runs the "program" body
  //   * `args` inside the body is null (main sets it *after* the initializer already ran)
  //   * code in static initializers may run interpreted on some JVMs (slow benchmarks)
  println(s"OldStyle.x = ${OldStyle.x}")  // prints "OldStyle body running" first, then 10
  println("Prefer @main or def main; `App` is kept for cross-building with Scala 2.")

// (2) Classic Java-compatible entry point. Run with --main-class basics.hello.JavaStyle
object JavaStyle:
  def main(args: Array[String]): Unit =
    println(s"JavaStyle.main called with ${args.mkString("[", ", ", "]")}")

// (3) App trait
object OldStyle extends App:
  val x = 10
  println("OldStyle body running")

// A user-defined argument type for @main: just provide a FromString given.
case class Port(n: Int)
given scala.util.CommandLineParser.FromString[Port] with
  def fromString(s: String): Port = Port(s.toInt)

// scala-cli run 01_hello.scala --main-class basics.hello.serve -- 8080
@main def serve(port: Port): Unit = println(s"serving on $port")

def section(title: String): Unit = println(s"\n=== $title ===")
