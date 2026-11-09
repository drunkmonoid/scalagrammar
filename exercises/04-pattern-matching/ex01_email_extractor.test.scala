//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/04-pattern-matching/02_extractors.scala

package exercises.patternmatching.emailextractor

// Task:
// Implement an `object Email` with an `unapply(s: String): Option[(String, String)]` extractor.
// It extracts `(user, domain)` from a valid email string containing exactly one '@' character,
// where both `user` and `domain` are non-empty.
// If the string contains no '@', multiple '@'s, or an empty user or domain, return None.
// Example: "alice@example.com" match { case Email(u, d) => s"$u on $d" } -> "alice on example.com"

object Email:
  def unapply(s: String): Option[(String, String)] = ???

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
