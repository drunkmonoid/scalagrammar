//> using scala 3.9.0
// See also: lessons/08-errors/01_exceptions_resources.scala
// Run: scala-cli run lessons/11-advanced/10_processes_and_io.scala
//
// Process execution via scala.sys.process and character I/O via scala.io.Source.

package advanced.processes

import scala.sys.process.*
import scala.io.Source
import scala.util.Using
import scala.compiletime.testing.typeCheckErrors
import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets

@main def run(): Unit =
  section("Process execution with scala.sys.process")
  // Run external command and capture stdout
  val out = "echo scalagrammar".!!.trim
  println(out) // scalagrammar

  section("Reading character sources with scala.io.Source and Using")
  val tempFile: Path = Files.createTempFile("scalagrammar-demo-", ".txt")
  try
    Files.writeString(tempFile, "scalagrammar file content", StandardCharsets.UTF_8)

    // ⚠ Source is not closed by the garbage collector on any schedule you can rely on;
    // always manage it deterministically through scala.util.Using.
    val readContent = Using.resource(Source.fromFile(tempFile.toFile, "UTF-8")) { src =>
      src.mkString.trim
    }
    println(readContent) // scalagrammar file content
  finally
    Files.deleteIfExists(tempFile)

  println(s"temp file deleted: ${!Files.exists(tempFile)}") // temp file deleted: true

  section("Compile-time safety with typeCheckErrors")
  // Process string interpolator / method returns String or ProcessBuilder, not an arbitrary type:
  doesNotCompile("""val bad: Int = "echo scalagrammar".!!""")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
