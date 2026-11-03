//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.0.3
// Run: scala-cli test ecosystem/testing/munit.test.scala
//      scala-cli test ecosystem/testing/munit.test.scala -- '*Fixtures*'   (filter by suite)
//
// MUnit: a small, fast testing library with great diffs, used by the Scala compiler itself
// and by scala-cli's own templates.

package ecosystem.testing.munitdemo

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// ---------- code under test ----------
object Slug:
  def apply(s: String): String =
    s.trim.toLowerCase.replaceAll("[^a-z0-9]+", "-").stripPrefix("-").stripSuffix("-")

// ---------- basic suite ----------
class SlugSuite extends munit.FunSuite:
  test("lowercases and joins words with dashes") {
    assertEquals(Slug("Hello World"), "hello-world")
  }

  test("strips punctuation at both ends") {
    assertEquals(Slug("  ¡Scala 3! "), "scala-3")
  }

  test("assertions: assert, assertNotEquals, assertNoDiff, intercept") {
    assert(Slug("a").nonEmpty, "slug should not be empty")
    assertNotEquals(Slug("a b"), "a b")
    assertNoDiff("  multi\n  line  ", "multi\nline")          // ignores whitespace differences
    val e = intercept[NumberFormatException]("x".toInt)
    assert(e.getMessage.contains("x"))
    interceptMessage[ArithmeticException]("/ by zero")(1 / 0)
  }

  test("expected failure, shown as passing".fail) {
    assertEquals(Slug("A"), "A")    // fails → test passes because it is marked .fail
  }

  test("ignored test".ignore) {
    ???
  }

  test("compile errors can be asserted") {
    assertNoDiff(
      compileErrors("val x: Int = \"s\""),
      """|error:
         |Found:    ("s" : String)
         |Required: Int
         |val x: Int = "s"
         |             ^
         |""".stripMargin
    )
  }

  // Futures are awaited automatically (default timeout 30s, override munitTimeout)
  test("async test with Future") {
    Future(Slug("Async Test")).map(s => assertEquals(s, "async-test"))
  }

// ---------- fixtures: per-test setup/teardown ----------
class FixturesSuite extends munit.FunSuite:
  val tempFile = FunFixture[java.nio.file.Path](
    setup = test => java.nio.file.Files.createTempFile("munit-", ".txt"),
    teardown = path => java.nio.file.Files.deleteIfExists(path)
  )

  tempFile.test("writes and reads a temp file") { path =>
    java.nio.file.Files.writeString(path, "content")
    assertEquals(java.nio.file.Files.readString(path), "content")
  }

  // Suite-level hooks
  override def beforeAll(): Unit = println("  [FixturesSuite] beforeAll")
  override def afterAll(): Unit = println("  [FixturesSuite] afterAll")

// ---------- parameterised tests ----------
class TableSuite extends munit.FunSuite:
  for (input, expected) <- List("A B" -> "a-b", "x" -> "x", "--y--" -> "y") do
    test(s"Slug($input) == $expected") {
      assertEquals(Slug(input), expected)
    }
