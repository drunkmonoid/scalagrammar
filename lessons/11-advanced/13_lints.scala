//> using scala 3.9.0
// See also: lessons/11-advanced/10_processes_and_io.scala
// Run: scala-cli run lessons/11-advanced/13_lints.scala
//
// Scalac compiler lints and flags:
// Running compiler warnings under `-Wunused:all`, `-Wvalue-discard`, `-Wnonunit-statement`,
// `-deprecation`, and `-feature`, followed by fatal warning enforcement via `-Werror`.
//
// Key Compiler Flags:
// | Flag            | Purpose                                                                   |
// |-----------------|---------------------------------------------------------------------------|
// | -explain        | Provides expanded, multi-line explanations and hints for compile errors   |
// | -source future  | Enables future language features and strict deprecation checks            |
// | -Xprint:typer   | Prints abstract syntax trees (ASTs) after the typer phase                 |
// | -Wconf          | Configures granular warning filters, categories, and actions              |
// | -Werror         | Treats all compiler warnings as fatal compilation errors                  |
// | -rewrite        | Automatically migrates source code syntax (e.g. with -new-syntax)        |

package advanced.lints

import scala.sys.process.*
import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets

def hasScalaCli: Boolean =
  try
    Process(Seq("which", "scala-cli")).!(ProcessLogger(_ => ())) == 0
  catch
    case _: Throwable => false

@main def run(): Unit =
  section("Scalac compiler flags reference")
  println("Documented compiler flags: -explain, -source future, -Xprint:typer, -Wconf, -Werror, -rewrite")

  if !hasScalaCli then
    println("scala-cli is not on PATH; skipping subprocess lint compilation.")
    return

  section("Creating temporary source with intentional lint warnings")
  val tempFile: Path = Files.createTempFile("lint-demo-", ".scala")
  try
    val sourceCode =
      """//> using scala 3.9.0
        |package lintsample
        |
        |import scala.collection.mutable.ListBuffer // unused import
        |
        |class Sample {
        |  private def unusedHelper(): Int = 42    // unused private method
        |
        |  @deprecated("Use newMethod instead", "1.0")
        |  def oldMethod(): Int = 100
        |
        |  def testDiscard(): Unit = {
        |    val x = 10 + 20                        // non-unit statement / discarded value
        |    oldMethod()                            // deprecated call
        |  }
        |
        |  implicit def intToString(n: Int): String = n.toString // feature warning
        |}
        |""".stripMargin

    Files.writeString(tempFile, sourceCode, StandardCharsets.UTF_8)
    println(s"Created temp source: $tempFile")

    section("Running scala-cli compile with lint flags")
    val compileCmd = Seq(
      "scala-cli", "compile", "--server=false", tempFile.toString,
      "--scalac-option", "-Wunused:all",
      "--scalac-option", "-Wvalue-discard",
      "--scalac-option", "-Wnonunit-statement",
      "--scalac-option", "-deprecation",
      "--scalac-option", "-feature"
    )

    val outputLines = collection.mutable.ListBuffer.empty[String]
    val logger = ProcessLogger(
      out => outputLines += out,
      err => outputLines += err
    )

    val exit1 = Process(compileCmd).!(logger)
    println(s"Compilation exit code: $exit1")

    val warnings = outputLines.toList
    val unusedWarnings = warnings.filter(l => l.toLowerCase.contains("unused") || l.toLowerCase.contains("never used"))
    val discardWarnings = warnings.filter(l => l.toLowerCase.contains("discard") || l.toLowerCase.contains("pure expression") || l.toLowerCase.contains("statement"))
    val deprecationWarnings = warnings.filter(l => l.toLowerCase.contains("deprecated") || l.toLowerCase.contains("deprecation"))
    val featureWarnings = warnings.filter(l => l.toLowerCase.contains("feature") || l.toLowerCase.contains("implicit conversion"))

    println(s"\n--- Unused warnings (${unusedWarnings.size} lines) ---")
    unusedWarnings.take(3).foreach(println)

    println(s"\n--- Discard / Non-unit warnings (${discardWarnings.size} lines) ---")
    discardWarnings.take(3).foreach(println)

    println(s"\n--- Deprecation warnings (${deprecationWarnings.size} lines) ---")
    deprecationWarnings.take(3).foreach(println)

    println(s"\n--- Feature warnings (${featureWarnings.size} lines) ---")
    featureWarnings.take(3).foreach(println)

    section("Enforcing fatal warnings with -Werror")
    val werrorCmd = Seq(
      "scala-cli", "compile", "--server=false", tempFile.toString,
      "--scalac-option", "-Wunused:all",
      "--scalac-option", "-Werror"
    )

    val werrorLines = collection.mutable.ListBuffer.empty[String]
    val exit2 = Process(werrorCmd).!(ProcessLogger(l => werrorLines += l, l => werrorLines += l))
    println(s"Compilation with -Werror exit code: $exit2 (compilation failed as expected: ${exit2 != 0})")
  finally
    Files.deleteIfExists(tempFile)
    println(s"Temp file deleted: ${!Files.exists(tempFile)}")

def section(title: String): Unit = println(s"\n=== $title ===")
