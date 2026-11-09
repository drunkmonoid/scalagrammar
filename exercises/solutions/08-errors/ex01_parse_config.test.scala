//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/08-errors/03_validation_accumulation.scala

package solutions.errors.parseconfig

case class Config(host: String, port: Int, ssl: Boolean)

def parseConfig(rawHost: String, rawPort: String, rawSsl: String): Either[List[String], Config] =
  val hostRes: Either[String, String] =
    if rawHost.trim.nonEmpty then Right(rawHost.trim)
    else Left("host is blank")

  val portRes: Either[String, Int] =
    rawPort.toIntOption match
      case Some(p) if p >= 1 && p <= 65535 => Right(p)
      case _                               => Left(s"invalid port: $rawPort")

  val sslRes: Either[String, Boolean] =
    rawSsl.toLowerCase match
      case "true"  => Right(true)
      case "false" => Right(false)
      case _       => Left(s"invalid ssl: $rawSsl")

  val errors = List(hostRes, portRes, sslRes).collect { case Left(err) => err }
  if errors.isEmpty then
    Right(Config(hostRes.toOption.get, portRes.toOption.get, sslRes.toOption.get))
  else
    Left(errors)

class ParseConfigSuite extends munit.FunSuite:
  test("parses valid configuration successfully") {
    val res = parseConfig("localhost", "8080", "true")
    assertEquals(res, Right(Config("localhost", 8080, true)))
  }

  test("accumulates errors for partial failures") {
    val res = parseConfig("  ", "abc", "false")
    assertEquals(res, Left(List("host is blank", "invalid port: abc")))
  }

  test("accumulates all three errors when all fields are invalid") { // ⚠
    val res = parseConfig("", "99999", "not_a_bool")
    val expected = List("host is blank", "invalid port: 99999", "invalid ssl: not_a_bool")
    assertEquals(res, Left(expected))
  }
