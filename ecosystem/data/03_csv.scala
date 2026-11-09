//> using scala 3.9.0
//> using dep co.fs2::fs2-data-csv:1.11.2
//> using dep co.fs2::fs2-data-csv-generic:1.11.2
//> using dep co.fs2::fs2-io:3.14.0
//> using dep org.typelevel::cats-effect:3.7.1
// See also: ecosystem/data/01_doobie.scala
// Run: scala-cli run ecosystem/data/03_csv.scala
//
// Streaming CSV processing with fs2-data-csv and fs2-data-csv-generic.
// Supports header-based encoding/decoding and lenient error recovery per row.

package ecosystem.data.csvdemo

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.Stream
import fs2.data.csv.*
import fs2.data.csv.generic.semiauto.*
import fs2.data.csv.lenient.*
import fs2.io.file.{Files, Flags, Path}

case class Metric(name: String, value: Int)

object Metric:
  given CsvRowEncoder[Metric, String] = deriveCsvRowEncoder
  given CsvRowDecoder[Metric, String] = deriveCsvRowDecoder

@main def run(): Unit =
  section("Write CSV with fs2-data-csv")
  val tempFile = java.nio.file.Files.createTempFile("fs2_data_metrics", ".csv")
  val fs2Path = Path.fromNioPath(tempFile)

  try
    val validRows = List(
      Metric("cpu_usage", 42),
      Metric("memory_mb", 1024)
    )

    // Write valid rows with CSV headers
    Stream.emits(validRows)
      .through(encodeUsingFirstHeaders(fullRows = true))
      .through(fs2.text.utf8.encode)
      .through(Files[IO].writeAll(fs2Path))
      .compile
      .drain
      .unsafeRunSync()

    // Append an invalid row: non-integer in the integer value column
    Stream.emit("disk_pct,not_an_int\n")
      .through(fs2.text.utf8.encode)
      .through(Files[IO].writeAll(fs2Path, Flags.Append))
      .compile
      .drain
      .unsafeRunSync()

    println(s"Written CSV to temporary file: $tempFile")

    section("Read and decode CSV with lenient error handling")
    val decodedRows = Files[IO]
      .readAll(fs2Path)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Metric]())
      .compile
      .toList
      .unsafeRunSync()

    decodedRows.foreach {
      case Right(metric) =>
        println(s"Valid row: ${metric.name} -> ${metric.value}") // Valid row: cpu_usage -> 42, Valid row: memory_mb -> 1024
      case Left(error) =>
        println(s"Decode error: ${error.getMessage}") // Decode error: ...
    }
  finally
    java.nio.file.Files.deleteIfExists(tempFile)
    println(s"Temporary file deleted: ${!java.nio.file.Files.exists(tempFile)}") // Temporary file deleted: true

def section(title: String): Unit = println(s"\n=== $title ===")
