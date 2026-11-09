//> using scala 3.9.0
//> using dep org.typelevel::log4cats-slf4j:2.7.0
//> using dep org.slf4j:slf4j-simple:2.0.16
//> using dep com.outr::scribe:3.16.0
//> using dep org.typelevel::cats-effect:3.7.1
// See also: ecosystem/json/02_upickle_jsoniter.scala
// Run: scala-cli run ecosystem/logging/01_log4cats_scribe.scala
//
// Modern Scala logging: effectful structured logging with log4cats and zero-dependency direct logging with Scribe.

package ecosystem.logging.demo

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scribe.Level
import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("log4cats: effectful logging with structured MDC context")
  val log4catsProgram = for
    logger <- Slf4jLogger.create[IO]
    // 1. Info log
    _ <- logger.info("log4cats info message")
    // 2. Structured log with correlation id in context
    _ <- logger.info(Map("correlation_id" -> "corr-xyz-789"))("processing operation")
  yield ()

  log4catsProgram.unsafeRunSync()

  section("Scribe: direct-style configurable logger")
  // Enable debug level on the root logger so debug statements are not dropped
  scribe.Logger.root
    .clearModifiers()
    .withMinimumLevel(Level.Debug)
    .replace()

  // Log at info and debug
  scribe.info("scribe info message")
  scribe.debug("scribe debug message with detailed diagnostic")

  section("Compile-time safety with typeCheckErrors")
  // StructuredLogger returns F[Unit] (IO[Unit]), not Unit directly:
  doesNotCompile("""
    import org.typelevel.log4cats.slf4j.Slf4jLogger
    import cats.effect.IO
    val bad: Unit = Slf4jLogger.getLogger[IO].info("fail")
  """)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
