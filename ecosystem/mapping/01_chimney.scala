//> using scala 3.9.0
//> using dep io.scalaland::chimney:1.7.3
// Run: scala-cli run ecosystem/mapping/01_chimney.scala
//
// Boilerplate-free data mapping with Chimney: automatic transformations, field renaming, and partial transformations.

package ecosystem.mapping.chimneydemo

import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.partial
import scala.compiletime.testing.typeCheckErrors

// 1. Identical fields
case class SourceUser(id: Int, name: String)
case class TargetUser(id: Int, name: String)

// 2. Renamed fields
case class Customer(customerId: Int, fullName: String)
case class Client(id: Int, name: String)

// 3. Partial transformation (string to int port)
case class RawConfig(host: String, port: String)
case class ServiceConfig(host: String, port: Int)

@main def run(): Unit =
  section("Automatic case class transformation")
  val src = SourceUser(1, "Ada")
  val target = src.transformInto[TargetUser]
  println(target) // TargetUser(1,Ada)

  section("Field renaming")
  val customer = Customer(42, "Bob")
  val client = customer
    .into[Client]
    .withFieldRenamed(_.customerId, _.id)
    .withFieldRenamed(_.fullName, _.name)
    .transform
  println(client) // Client(42,Bob)

  section("Partial transformation with error handling")
  val badRaw = RawConfig("localhost", "no")
  val partialResult = badRaw
    .intoPartial[ServiceConfig]
    .withFieldComputedPartial(
      _.port,
      cfg => partial.Result.fromOption(cfg.port.toIntOption)
    )
    .transform

  val output = partialResult match
    case partial.Result.Value(v)   => s"unexpected success: $v"
    case partial.Result.Errors(es) =>
      // Prints partial transformation error without throwing out of @main
      es.asErrorPathMessageStrings.mkString(", ")

  println(output) // port: empty value

  section("Compile-time safety with typeCheckErrors")
  // Transforming between types with mismatched field names without configuration fails compilation:
  doesNotCompile("""
    val c = Customer(1, "test")
    c.transformInto[Client]
  """)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
