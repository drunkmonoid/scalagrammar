//> using scala 3.9.0
//> using dep io.github.iltotore::iron:3.4.1
//> using dep io.github.iltotore::iron-circe:3.4.1
//> using dep io.circe::circe-core:0.14.17
//> using dep io.circe::circe-generic:0.14.17
//> using dep io.circe::circe-parser:0.14.17
// See also: ecosystem/refined/01_iron.scala
// Run: scala-cli run ecosystem/json/03_iron_circe.scala
//
// Refined JSON decoding with Iron and circe.
// Iron constraints validate fields at runtime during circe decoding,
// accumulating or reporting validation errors without throwing.

package ecosystem.json.ironcirce

import io.circe.*
import io.circe.parser.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.circe.given

type Email = String :| Match["^[^@\\s]+@[^@\\s]+\\.[a-z]+$"]
type Age = Int :| Positive

case class UserProfile(email: Email, age: Age) derives Codec.AsObject

@main def run(): Unit =
  section("Successful decode with valid Iron constraints")
  val validJson = """{"email": "alice@example.com", "age": 30}"""
  decode[UserProfile](validJson) match
    case Right(profile) =>
      println(s"Successfully decoded: email=${profile.email}, age=${profile.age}") // Successfully decoded: email=alice@example.com, age=30
    case Left(err) =>
      println(s"Unexpected decode error: $err")

  section("Failed decode with invalid Iron constraints")
  val invalidJson = """{"email": "nope", "age": -1}"""
  decode[UserProfile](invalidJson) match
    case Right(profile) =>
      println(s"Unexpected success: $profile")
    case Left(err) =>
      // Prints the circe error without throwing out of @main
      println(s"Caught expected decode failure: ${err.getMessage}") // Caught expected decode failure: ...

def section(title: String): Unit = println(s"\n=== $title ===")
