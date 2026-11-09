//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/07-contextual/07_notgiven.scala

package solutions.contextual.atleastone

import scala.util.NotGiven

trait AtLeastOne[A, B]

object AtLeastOne:
  given [A, B](using NotGiven[(A =:= Unit, B =:= Unit)]): AtLeastOne[A, B] = new AtLeastOne[A, B] {}

def requirePayload[A, B](a: A, b: B)(using AtLeastOne[A, B]): String = "valid"

class AtLeastOneSuite extends munit.FunSuite:
  test("succeeds when neither is Unit") {
    assertEquals(requirePayload(42, "hello"), "valid")
  }

  test("succeeds when exactly one type is Unit") {
    assertEquals(requirePayload((), "payload"), "valid")
    assertEquals(requirePayload(123, ()), "valid")
  }

  test("fails to compile when both types are Unit") { // ⚠
    assert(compileErrors("requirePayload((), ())").nonEmpty)
  }
