//> using scala 3.9.0
//> using dep io.circe::circe-core:0.14.17
//> using dep io.circe::circe-generic:0.14.17
//> using dep io.circe::circe-parser:0.14.17
// Run: scala-cli run ecosystem/json/01_circe.scala
//
// circe: the Typelevel JSON library. JSON is an immutable ADT (Json); conversion happens
// through the Encoder[A] / Decoder[A] type classes, which you derive or write by hand.

package ecosystem.json.circedemo

import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

// ---------- derivation with `derives` (Scala 3) ----------
case class Address(street: String, city: String) derives Encoder.AsObject, Decoder
case class User(name: String, age: Int, email: Option[String], address: Address, tags: List[String])
  derives Encoder.AsObject, Decoder

// Enums / sealed hierarchies: by default encoded as {"CaseName": {...}}
enum Shape derives Encoder.AsObject, Decoder:
  case Circle(radius: Double)
  case Rect(w: Double, h: Double)

// ---------- hand-written codecs ----------
final case class Money(cents: Long)
object Money:
  // Encode as a decimal string "12.34"
  given Encoder[Money] = Encoder.encodeString.contramap(m => f"${m.cents / 100.0}%.2f")
  given Decoder[Money] = Decoder.decodeString.emap { s =>
    s.toDoubleOption.map(d => Money(math.round(d * 100))).toRight(s"not a money amount: $s")
  }

case class Order(id: Int, total: Money) derives Encoder.AsObject, Decoder

// Custom field names / shape
case class Legacy(firstName: String, lastName: String)
object Legacy:
  given Encoder[Legacy] = Encoder.forProduct2("first_name", "last_name")(l => (l.firstName, l.lastName))
  given Decoder[Legacy] = Decoder.forProduct2("first_name", "last_name")(Legacy.apply)

@main def run(): Unit =
  section("Building JSON by hand")
  val j = Json.obj(
    "name" -> Json.fromString("Ada"),
    "langs" -> Json.arr(Json.fromString("scala"), Json.fromString("haskell")),
    "active" -> Json.True,
    "score" -> Json.fromDoubleOrNull(9.5),
  )
  println(j.spaces2)
  println(j.noSpaces)

  section("Encoding case classes")
  val ada = User("Ada", 36, None, Address("Main St", "London"), List("math", "computing"))
  println(ada.asJson.spaces2)
  println(ada.asJson.deepDropNullValues.noSpaces)          // drop "email": null
  println((Shape.Circle(1): Shape).asJson.noSpaces)
  println(List(Order(1, Money(1250)), Order(2, Money(99))).asJson.noSpaces)
  println(Legacy("Grace", "Hopper").asJson.noSpaces)

  section("Parsing & decoding")
  val raw = """{"name":"Bob","age":41,"email":"bob@x.org","address":{"street":"High St","city":"Leeds"},"tags":[]}"""
  println(decode[User](raw))
  println(decode[User]("""{"name":"Bob"}"""))                // DecodingFailure with a path
  println(decode[User]("""{"name": oops}"""))                // ParsingFailure
  println(decode[Order]("""{"id":3,"total":"7.50"}"""))
  println(decode[Order]("""{"id":3,"total":"lots"}"""))
  println(decode[Shape]("""{"Rect":{"w":2,"h":3}}"""))
  println(decode[Legacy]("""{"first_name":"Alan","last_name":"Turing"}"""))

  section("Accumulating decoding errors")
  // decode stops at the first error; decodeAccumulating reports all of them
  println(decodeAccumulating[User]("""{"name":1,"age":"x","address":{},"tags":[]}""").leftMap(_.toList.map(_.getMessage)))

  section("Navigating with cursors")
  val doc = parse(raw).getOrElse(Json.Null)
  val c = doc.hcursor
  println(c.downField("address").downField("city").as[String])
  println(c.downField("age").as[Int].map(_ + 1))
  println(c.downField("nope").as[String].left.map(_.getMessage))
  val modified = c.downField("age").withFocus(j => j.as[Int].fold(_ => j, a => Json.fromInt(a + 1))).top
  println(modified.map(_.noSpaces))

  section("Transforming JSON")
  println(doc.mapObject(_.remove("tags").add("updated", Json.True)).noSpaces)
  println(doc.findAllByKey("city"))
  println(Json.obj("a" -> Json.fromInt(1)).deepMerge(Json.obj("b" -> Json.fromInt(2))).noSpaces)

  section("Pitfalls")
  // * Option[A] fields encode as null by default; use .deepDropNullValues or a Printer.
  // * Large Longs (> 2^53) lose precision in JavaScript clients — encode as strings.
  // * Default values of case class params are ignored by plain derivation (use
  //   circe-generic's ConfiguredCodec / Configuration.default.withDefaults).
  println(Printer.noSpaces.copy(dropNullValues = true).print(ada.asJson))

def section(title: String): Unit = println(s"\n=== $title ===")
