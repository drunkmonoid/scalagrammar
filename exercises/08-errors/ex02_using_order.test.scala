//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/08-errors/01_exceptions_resources.scala

package exercises.errors.usingorder

import scala.collection.mutable.ListBuffer
import scala.util.Using

// Task:
// Implement `class TrackedResource(val name: String, events: ListBuffer[String]) extends AutoCloseable`:
//   `def close(): Unit = events += s"close:$name"`
// Implement `def runWithThreeResources(events: ListBuffer[String], shouldThrow: Boolean): Unit`:
//   Open three TrackedResource instances ("r1", "r2", "r3") using `scala.util.Using.Manager`.
//   If `shouldThrow` is true, throw new RuntimeException("boom") inside the manager block.
//   The test verifies that resources are always closed in reverse order of acquisition ("close:r3", "close:r2", "close:r1"),
//   even when the body throws an exception.

class TrackedResource(val name: String, events: ListBuffer[String]) extends AutoCloseable:
  def close(): Unit = events += s"close:$name"

def runWithThreeResources(events: ListBuffer[String], shouldThrow: Boolean): Unit = ???

class UsingOrderSuite extends munit.FunSuite:
  test("closes resources in reverse order on normal exit") {
    val events = scala.collection.mutable.ListBuffer.empty[String]
    runWithThreeResources(events, shouldThrow = false)
    assertEquals(events.toList, List("close:r3", "close:r2", "close:r1"))
  }

  test("closes resources in reverse order when an exception is thrown") { // ⚠
    val events = scala.collection.mutable.ListBuffer.empty[String]
    runWithThreeResources(events, shouldThrow = true)
    assertEquals(events.toList, List("close:r3", "close:r2", "close:r1"))
  }

  test("preserves reverse cleanup across repeated independent runs") {
    val events = scala.collection.mutable.ListBuffer.empty[String]
    runWithThreeResources(events, shouldThrow = false)
    runWithThreeResources(events, shouldThrow = false)
    val expected = List(
      "close:r3", "close:r2", "close:r1",
      "close:r3", "close:r2", "close:r1"
    )
    assertEquals(events.toList, expected)
  }
