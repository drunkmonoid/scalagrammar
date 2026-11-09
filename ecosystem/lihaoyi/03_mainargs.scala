//> using scala 3.9.0
//> using dep com.lihaoyi::mainargs:0.7.6
// See also: lessons/01-basics/01_hello.scala
// Run: scala-cli run ecosystem/lihaoyi/03_mainargs.scala
//
// CLI argument parsing with mainargs: subcommands, flags, type conversion, and usage docs.

package ecosystem.lihaoyi.mainargsdemo

import mainargs.{main, arg, Flag, ParserForMethods}
import scala.compiletime.testing.typeCheckErrors

object CliCommands:
  @main
  def serve(
    @arg(short = 'p', doc = "port to bind") port: Int = 8080,
    @arg(doc = "run in verbose mode") verbose: Flag = Flag()
  ): String =
    s"port=$port verbose=${verbose.value}"

@main def run(): Unit =
  section("Valid subcommand and flag parsing")
  val parser = ParserForMethods(CliCommands)

  // Parse in-process without reading real command-line arguments:
  val validArgs = Seq("serve", "-p", "9000", "--verbose")
  val parsed = parser.runEither(validArgs) match
    case Right(msg: String) => msg
    case Left(err)          => s"unexpected error: $err"

  println(parsed) // port=9000 verbose=true

  section("Invalid arguments and usage text handling")
  // Invalid port format ("abc" cannot be parsed as Int)
  val invalidArgs = Seq("serve", "-p", "abc")
  val usageOutput = parser.runEither(invalidArgs) match
    case Right(v)   => s"unexpected success: $v"
    case Left(help) =>
      // Print first line of usage/error output without crashing
      help.linesIterator.next()

  println(usageOutput) // Missing or invalid argument: -p <int>

  section("Compile-time safety with typeCheckErrors")
  // ParserForMethods requires an object with @main methods:
  doesNotCompile("""
    class NotAnObject
    mainargs.ParserForMethods(new NotAnObject)
  """)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
