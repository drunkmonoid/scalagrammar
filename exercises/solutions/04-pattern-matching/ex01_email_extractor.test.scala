//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/04-pattern-matching/02_extractors.scala

package solutions.patternmatching.emailextractor

object Email:
  def unapply(s: String): Option[(String, String)] =
    val parts = s.split("@", -1)
    if parts.length == 2 && parts(0).nonEmpty && parts(1).nonEmpty then
      Some((parts(0), parts(1)))
    else
      None

class EmailExtractorSuite extends munit.FunSuite:
  test("extracts valid email address into user and domain") {
    "alice@example.com" match
      case Email(user, domain) =>
        assertEquals(user, "alice")
        assertEquals(domain, "example.com")
      case _ => fail("expected Email match")
  }

  test("rejects strings without @ or with empty parts") { // ⚠
    assert(Email.unapply("invalid").isEmpty)
    assert(Email.unapply("@domain.com").isEmpty)
    assert(Email.unapply("user@").isEmpty)
    assert(Email.unapply("").isEmpty)
  }

  test("rejects strings with multiple @ symbols") { // ⚠
    assert(Email.unapply("user@foo@bar.com").isEmpty)
  }
