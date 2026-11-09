//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/08-errors/03_validation_accumulation.scala

package exercises.errors.parseconfig

// Task:
// Given `case class Config(host: String, port: Int, ssl: Boolean)`.
// Implement `parseConfig(rawHost: String, rawPort: String, rawSsl: String): Either[List[String], Config]`.
// Validation rules:
// - host: must not be blank ("host is blank")
// - port: must be integer in 1..65535 ("invalid port: $rawPort")
// - ssl: must be "true" or "false" ("invalid ssl: $rawSsl")
// Accumulate all errors so that if all three fields are invalid, all three errors are returned in a List.
// ⚠ If all three fields are invalid, exactly three errors are reported.

case class Config(host: String, port: Int, ssl: Boolean)

def parseConfig(rawHost: String, rawPort: String, rawSsl: String): Either[List[String], Config] = ???

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
