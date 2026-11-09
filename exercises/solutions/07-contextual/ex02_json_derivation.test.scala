//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/07-contextual/03_derivation.scala

package solutions.contextual.jsonderivation

import scala.deriving.Mirror
import scala.compiletime.{constValue, erasedValue, summonInline}

trait ToJson[A]:
  def toJson(a: A): String

object ToJson:
  def apply[A](using j: ToJson[A]): ToJson[A] = j

  given ToJson[String] with
    def toJson(s: String): String = s""""$s""""

  given ToJson[Int] with
    def toJson(i: Int): String = i.toString

  inline def summonAll[T <: Tuple]: List[ToJson[?]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[ToJson[t]] :: summonAll[ts]

  inline def fieldNames[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => constValue[t].toString :: fieldNames[ts]

  inline given derived[A](using m: Mirror.ProductOf[A]): ToJson[A] =
    val names = fieldNames[m.MirroredElemLabels]
    val encoders = summonAll[m.MirroredElemTypes]
    new ToJson[A]:
      def toJson(a: A): String =
        val p = a.asInstanceOf[Product]
        val fields = names.zip(encoders).zipWithIndex.map { case ((name, enc), idx) =>
          val elem = p.productElement(idx)
          val jsonVal = enc.asInstanceOf[ToJson[Any]].toJson(elem)
          s""""$name": $jsonVal"""
        }
        fields.mkString("{", ", ", "}")

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
