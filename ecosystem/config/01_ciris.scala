//> using scala 3.9.0
//> using dep is.cir::ciris:3.7.0
//> using dep org.typelevel::cats-effect:3.7.1
// See also: lessons/08-errors/03_validation_accumulation.scala
// Run: scala-cli run ecosystem/config/01_ciris.scala
//
// Functional configuration loading with Ciris and error accumulation.

package ecosystem.config.cirisdemo

import ciris.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets
import scala.compiletime.testing.typeCheckErrors

final case class ServerConfig(host: String, port: Int)

// Custom non-empty decoder for String
opaque type NonEmptyString = String
object NonEmptyString:
  def apply(s: String): Option[NonEmptyString] =
    Option.when(s.trim.nonEmpty)(s.trim)

given ConfigDecoder[String, NonEmptyString] =
  ConfigDecoder[String, String].mapOption("non-empty string")(NonEmptyString.apply)

@main def run(): Unit =
  section("Success path: loading configuration from a file")
  val tempFile: Path = Files.createTempFile("ciris-config-", ".txt")
  try
    Files.writeString(tempFile, "127.0.0.1\n8080\n", StandardCharsets.UTF_8)

    val validConfig = (
      file(tempFile.toFile).map(_.linesIterator.next()).as[NonEmptyString],
      file(tempFile.toFile).map(_.linesIterator.drop(1).next()).as[Int]
    ).parMapN((h, p) => ServerConfig(h, p))

    val serverConfig = validConfig.load[IO].unsafeRunSync()
    println(s"host=${serverConfig.host} port=${serverConfig.port}") // host=127.0.0.1 port=8080
  finally
    Files.deleteIfExists(tempFile)

  println(s"temp file deleted: ${!Files.exists(tempFile)}") // temp file deleted: true

  section("Failure path: accumulating multiple configuration errors")
  // Both properties will fail: host is empty, port is not an integer
  sys.props("app.host") = "   "
  sys.props("app.port") = "not-an-integer"

  val badConfig = (
    prop("app.host").as[NonEmptyString],
    prop("app.port").as[Int]
  ).parMapN((h, p) => ServerConfig(h, p))

  val errorSummary = badConfig.attempt[IO].unsafeRunSync() match
    case Left(err) =>
      // Ciris accumulates errors and prints all failed keys
      err.toString
    case Right(v)  =>
      s"unexpected success: $v"

  println(errorSummary)

  section("Compile-time safety with typeCheckErrors")
  // ConfigValue cannot be assigned to an unwrapped type without calling .load or .attempt:
  doesNotCompile("""
    val bad: Int = ciris.prop("app.port")
  """)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
