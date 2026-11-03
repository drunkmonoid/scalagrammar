//> using scala 3.9.0
//> using dep org.http4s::http4s-ember-server:0.23.38
//> using dep org.http4s::http4s-ember-client:0.23.38
//> using dep org.http4s::http4s-dsl:0.23.38
//> using dep org.http4s::http4s-circe:0.23.38
//> using dep io.circe::circe-generic:0.14.17
//> using dep org.slf4j:slf4j-simple:2.0.20
// Run: scala-cli run ecosystem/http/01_http4s.scala
//
// http4s: a Typelevel HTTP library. A server is just a function
//   HttpRoutes[IO] ≈ Request[IO] => OptionT[IO, Response[IO]]
// built with a pattern-matching DSL; middleware are functions from routes to routes.
// This program starts an Ember server on a random free port, calls it with the Ember client,
// prints the results, and shuts everything down (Resource handles the lifecycle).

package ecosystem.http.http4sdemo

import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.*
import io.circe.{Decoder, Encoder, Json}
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.Logger
import org.http4s.client.Client

case class Todo(id: Int, title: String, done: Boolean) derives Encoder.AsObject, Decoder
case class NewTodo(title: String) derives Decoder, Encoder.AsObject

// A query parameter matcher: ?limit=10
object LimitParam extends OptionalQueryParamDecoderMatcher[Int]("limit")

def routes(store: Ref[IO, Map[Int, Todo]]): HttpRoutes[IO] = HttpRoutes.of[IO] {
  case GET -> Root / "health" =>
    Ok("ok")

  case GET -> Root / "todos" :? LimitParam(limit) =>
    store.get.flatMap(m => Ok(m.values.toList.sortBy(_.id).take(limit.getOrElse(100))))

  case GET -> Root / "todos" / IntVar(id) =>              // path extractor: only matches ints
    store.get.map(_.get(id)).flatMap {
      case Some(todo) => Ok(todo)
      case None       => NotFound(s"todo $id not found")
    }

  case req @ POST -> Root / "todos" =>
    // attemptAs gives Either[DecodeFailure, A], so a bad body becomes a 400 with the reason
    req.attemptAs[NewTodo].value.flatMap {
      case Left(err) => BadRequest(err.getMessage)
      case Right(nt) =>
        store.modify { m =>
          val t = Todo(m.size + 1, nt.title, done = false)
          (m + (t.id -> t), t)
        }.flatMap(Created(_))
    }

  case DELETE -> Root / "todos" / IntVar(id) =>
    store.update(_ - id) *> NoContent()
}

object Main extends IOApp.Simple:
  def run: IO[Unit] =
    val app = for
      store  <- Resource.eval(Ref.of[IO, Map[Int, Todo]](Map(1 -> Todo(1, "learn Scala", done = true))))
      httpApp = Logger.httpApp(logHeaders = false, logBody = false)(routes(store).orNotFound)
      server <- EmberServerBuilder.default[IO].withHost(host"127.0.0.1").withPort(port"0")
                  .withHttpApp(httpApp).build
      client <- EmberClientBuilder.default[IO].build
    yield (server.baseUri, client)

    app.use { (base, client) =>
      def show(label: String, req: Request[IO]): IO[Unit] =
        client.run(req).use(r => r.bodyText.compile.string.flatMap(b => IO.println(f"  $label%-28s ${r.status.code} $b")))
      for
        _ <- IO.println(s"=== server listening on $base ===")
        _ <- show("GET /health", Request(uri = base / "health"))
        _ <- show("GET /todos", Request(uri = base / "todos"))
        _ <- show("POST /todos", Request[IO](Method.POST, base / "todos").withEntity(NewTodo("write lessons")))
        _ <- show("POST /todos (bad JSON)", Request[IO](Method.POST, base / "todos").withEntity(Json.obj("oops" -> Json.fromInt(1))))
        _ <- show("GET /todos/2", Request(uri = base / "todos" / "2"))
        _ <- show("GET /todos/99", Request(uri = base / "todos" / "99"))
        _ <- show("GET /todos/abc (no route)", Request(uri = base / "todos" / "abc"))
        _ <- show("GET /todos?limit=1", Request(uri = (base / "todos").withQueryParam("limit", 1)))
        _ <- show("DELETE /todos/1", Request(Method.DELETE, base / "todos" / "1"))
        // Typed client calls: decode the response body directly
        todos <- client.expect[List[Todo]](base / "todos")
        _ <- IO.println(s"  typed client.expect → $todos")
      yield ()
    }
