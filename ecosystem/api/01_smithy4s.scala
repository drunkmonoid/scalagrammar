//> using scala 3.9.0
//> using dep com.disneystreaming.smithy4s::smithy4s-dynamic:0.18.27
//> using dep software.amazon.smithy:smithy-model:1.52.0
// See also: ecosystem/json/01_circe.scala
// Run: scala-cli run ecosystem/api/01_smithy4s.scala
//
// Dynamic Smithy model interpretation with Smithy4s (no build-time code generator):
// 1. In-memory Smithy 2.0 IDL parsing using Amazon's Model.assembler()
// 2. DynamicSchemaIndex runtime model compilation
// 3. Schema-driven Document encoding and decoding
// 4. Validation error reporting on invalid documents

package ecosystem.api.smithy4sdemo

import software.amazon.smithy.model.Model
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.ShapeId
import smithy4s.Document

@main def run(): Unit =
  section("1. Assembling Smithy model dynamically at runtime")
  val smithySource =
    """$version: "2.0"
      |namespace example.api
      |
      |structure Book {
      |  @required
      |  id: String,
      |  @required
      |  title: String,
      |  pages: Integer
      |}
      |""".stripMargin

  val model = Model.assembler()
    .addUnparsedModel("example/book.smithy", smithySource)
    .assemble()
    .unwrap()

  val index = DynamicSchemaIndex.loadModel(model)
  val bookShapeId = ShapeId("example.api", "Book")
  val schema = index.getSchema(bookShapeId).getOrElse(sys.error(s"Shape $bookShapeId not found in index"))
  println(s"Loaded dynamic schema for shape: ${schema.shapeId}")

  section("2. Decoding valid Document against dynamic schema")
  val validDoc = Document.obj(
    "id" -> Document.fromString("book-42"),
    "title" -> Document.fromString("Structure and Interpretation of Computer Programs"),
    "pages" -> Document.fromInt(657)
  )
  println(s"Input Document:\n  $validDoc")

  val decoder = Document.Decoder.fromSchema(schema)
  val decodedResult = decoder.decode(validDoc)
  println(s"Decoded result:\n  $decodedResult")
  assert(decodedResult.isRight)

  section("3. Encoding back to Document / JSON representation")
  val dynamicValue = decodedResult.toOption.get
  val encoder = Document.Encoder.fromSchema(schema)
  val encodedDoc = encoder.encode(dynamicValue)
  println(s"Encoded Document:\n  $encodedDoc")
  assert(encodedDoc == validDoc)

  section("4. Decoding invalid Document missing required field")
  // Missing required 'title' field:
  val invalidDoc = Document.obj(
    "id" -> Document.fromString("book-bad")
  )
  println(s"Invalid Document (missing required title):\n  $invalidDoc")

  val invalidResult = decoder.decode(invalidDoc)
  println(s"Decode error received as expected:\n  $invalidResult")
  assert(invalidResult.isLeft)

  println("\nSmithy4s dynamic schema loading and validation completed successfully.")

def section(title: String): Unit = println(s"\n=== $title ===")
