//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/07-contextual/03_derivation.scala

package exercises.contextual.jsonderivation

import scala.deriving.Mirror
import scala.compiletime.{constValue, erasedValue, summonInline}

// Task:
// Define a tiny type class `trait ToJson[A] { def toJson(a: A): String }`.
// Derive `ToJson[A]` for case classes (Mirror.ProductOf[A]) with String and Int fields.
// The output JSON object should format as: {"field1": value1, "field2": value2}

trait ToJson[A]:
  def toJson(a: A): String

object ToJson:
  def apply[A](using j: ToJson[A]): ToJson[A] = j

  given ToJson[String] with
    def toJson(s: String): String = s""""$s""""

  given ToJson[Int] with
    def toJson(i: Int): String = i.toString

  inline given derived[A](using m: Mirror.ProductOf[A]): ToJson[A] = ???

case class Person(name: String, age: Int)
case class SingleField(id: Int)

class JsonDerivationSuite extends munit.FunSuite:
  test("encodes primitive types") {
    assertEquals(ToJson[String].toJson("hello"), "\"hello\"")
    assertEquals(ToJson[Int].toJson(42), "42")
  }

  test("derives ToJson for multi-field case class") {
    val p = Person("Alice", 30)
    assertEquals(ToJson[Person].toJson(p), "{\"name\": \"Alice\", \"age\": 30}")
  }

  test("derives ToJson for single-field case class") { // ⚠
    val s = SingleField(100)
    assertEquals(ToJson[SingleField].toJson(s), "{\"id\": 100}")
  }
