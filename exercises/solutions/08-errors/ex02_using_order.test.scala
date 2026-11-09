//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/08-errors/01_exceptions_resources.scala

package solutions.errors.usingorder

import scala.collection.mutable.ListBuffer
import scala.util.Using

class TrackedResource(val name: String, events: ListBuffer[String]) extends AutoCloseable:
  def close(): Unit = events += s"close:$name"

def runWithThreeResources(events: ListBuffer[String], shouldThrow: Boolean): Unit =
  Using.Manager { use =>
    val r1 = use(new TrackedResource("r1", events))
    val r2 = use(new TrackedResource("r2", events))
    val r3 = use(new TrackedResource("r3", events))
    if shouldThrow then throw new RuntimeException("boom")
  }

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
