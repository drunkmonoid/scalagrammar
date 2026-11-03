//> using scala 3.9.0
//> using jvm 21
//> using dep com.softwaremill.sttp.tapir::tapir-netty-server-sync:1.14.1
//> using dep com.softwaremill.sttp.tapir::tapir-json-circe:1.14.1
//> using dep com.softwaremill.sttp.tapir::tapir-openapi-docs:1.14.1
//> using dep com.softwaremill.sttp.tapir::tapir-sttp-client4:1.14.1
//> using dep com.softwaremill.sttp.apispec::openapi-circe-yaml:0.12.1
//> using dep com.softwaremill.sttp.client4::core:4.1.1
//> using dep io.circe::circe-generic:0.14.17
//> using dep org.slf4j:slf4j-nop:2.0.20
// Run: scala-cli run ecosystem/http/02_tapir_sttp.scala
//
// tapir: describe HTTP endpoints as VALUES (inputs, outputs, errors), then INTERPRET them as
//   * a server (Netty, http4s, ZIO HTTP, Pekko HTTP, ...)
//   * a client (sttp)
//   * documentation (OpenAPI / Swagger UI)
// sttp client: a uniform HTTP client API over many backends (sync, Future, cats-effect, ZIO).
// This program serves the endpoints on a random port (Netty, direct style on virtual threads),
// calls them with both a raw sttp request and a typed client derived from the same endpoint,
// and prints the generated OpenAPI spec.

package ecosystem.http.tapirdemo

import io.circe.{Decoder, Encoder}
import sttp.client4.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.client.sttp4.SttpClientInterpreter
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.apispec.openapi.circe.yaml.*
import sttp.tapir.server.netty.sync.NettySyncServer
import scala.collection.concurrent.TrieMap
import ox.supervised

case class Book(title: String, year: Int) derives Encoder.AsObject, Decoder, Schema

// ---------- endpoint descriptions: pure values, no logic ----------
val getBook: PublicEndpoint[Int, String, Book, Any] =
  endpoint.get.in("books" / path[Int]("id"))
    .errorOut(statusCode(StatusCode.NotFound).and(stringBody))
    .out(jsonBody[Book])
    .description("Find a book by id")

val addBook: PublicEndpoint[Book, Unit, Int, Any] =
  endpoint.post.in("books").in(jsonBody[Book])
    .out(statusCode(StatusCode.Created).and(plainBody[Int]))

val searchBooks: PublicEndpoint[Option[Int], Unit, List[Book], Any] =
  endpoint.get.in("books").in(query[Option[Int]]("minYear")).out(jsonBody[List[Book]])

@main def run(): Unit =
  // ---------- server logic, attached to the descriptions ----------
  val db = TrieMap(1 -> Book("Structure and Interpretation", 1985))
  val serverEndpoints = List(
    getBook.handle(id => db.get(id).toRight(s"no book with id $id")),
    addBook.handleSuccess { b => val id = db.size + 1; db.put(id, b); id },
    searchBooks.handleSuccess(min => db.values.filter(b => min.forall(b.year >= _)).toList.sortBy(_.year)),
  )
  // The direct-style Netty server runs its connections on virtual threads owned by an Ox
  // scope (see ecosystem/direct-style/ox.scala), so it must be started inside `supervised`.
  supervised {
    val server = NettySyncServer().host("127.0.0.1").port(0).addEndpoints(serverEndpoints).start()
    val base = uri"http://127.0.0.1:${server.port}"
    println(s"=== server on $base ===")

    try
      val backend = DefaultSyncBackend()

      println("\n=== raw sttp requests ===")
      val r1 = basicRequest.get(uri"$base/books/1").send(backend)
      println(s"  GET /books/1   → ${r1.code} ${r1.body}")
      val r2 = basicRequest.get(uri"$base/books/42").send(backend)
      println(s"  GET /books/42  → ${r2.code} ${r2.body}")
      val r3 = basicRequest.post(uri"$base/books").body("""{"title":"bad"}""").send(backend)
      println(s"  POST invalid   → ${r3.code} ${r3.body}")     // tapir answers 400 with the reason

      println("\n=== typed client derived from the SAME endpoint values ===")
      val client = SttpClientInterpreter()
      def call[I, E, O](e: PublicEndpoint[I, E, O, Any], in: I): Either[E, O] =
        client.toRequestThrowDecodeFailures(e, Some(base)).apply(in).send(backend).body
      println(s"  addBook       → ${call(addBook, Book("Programming in Scala", 2008))}")
      println(s"  addBook       → ${call(addBook, Book("Functional Programming in Scala", 2014))}")
      println(s"  getBook(2)    → ${call(getBook, 2)}")
      println(s"  getBook(9)    → ${call(getBook, 9)}")
      println(s"  search(2000)  → ${call(searchBooks, Some(2000))}")

      println("\n=== OpenAPI documentation generated from the endpoints ===")
      val yaml = OpenAPIDocsInterpreter().toOpenAPI(List(getBook, addBook, searchBooks), "Books API", "1.0").toYaml
      println(yaml.linesIterator.take(25).map("  " + _).mkString("\n"))
      println("  ...")
    finally server.stop()
  }
