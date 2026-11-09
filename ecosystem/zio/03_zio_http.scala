//> using scala 3.9.0
//> using dep dev.zio::zio:2.1.27
//> using dep dev.zio::zio-http:3.0.1
//> using dep dev.zio::zio-json:0.7.3
// See also: ecosystem/zio/01_zio.scala
// Run: scala-cli run ecosystem/zio/03_zio_http.scala
//
// ZIO HTTP and zio-json:
// 1. Defining REST routes with type-safe Path DSL and JSON codecs
// 2. Binding to dynamic port 0 and discovering bound port via Server.port
// 3. Invoking endpoints via ZIO HTTP Client
// 4. Handling 404 Not Found responses
// 5. Automatic server shutdown when ZIO.scoped closes

package ecosystem.ziodemo.http

import zio.*
import zio.http.*
import zio.json.*

case class User(id: Long, name: String, role: String) derives JsonCodec

object ZioHttpApp extends ZIOAppDefault:

  val routes: Routes[Any, Response] = Routes(
    Method.GET / "health" -> handler(Response.text("OK")),
    Method.GET / "users" / "current" -> handler {
      val user = User(100L, "Grace Hopper", "Pioneer")
      Response.json(user.toJson)
    }
  )

  def run =
    val program = ZIO.scoped {
      for
        _ <- Console.printLine("=== Starting ZIO HTTP server on ephemeral port ===")
        _ <- Server.serve(routes).forkScoped
        port <- Server.port
        _ <- Console.printLine(s"Server bound and listening on port $port")

        client <- ZIO.service[Client]
        baseUri = s"http://localhost:$port"

        _ <- Console.printLine("\n=== 1. Requesting plain text route ===")
        res1 <- Client.request(Request.get(URL.decode(s"$baseUri/health").toOption.get))
        body1 <- res1.body.asString
        _ <- Console.printLine(s"GET /health -> Status: ${res1.status}, Body: '$body1'")
        _ <- ZIO.attempt(assert(res1.status == Status.Ok && body1 == "OK"))

        _ <- Console.printLine("\n=== 2. Requesting JSON route with zio-json codec ===")
        res2 <- Client.request(Request.get(URL.decode(s"$baseUri/users/current").toOption.get))
        body2 <- res2.body.asString
        decodedUser = body2.fromJson[User]
        _ <- Console.printLine(s"GET /users/current -> Status: ${res2.status}, Raw JSON: $body2")
        _ <- Console.printLine(s"Decoded User: $decodedUser")
        _ <- ZIO.attempt(assert(res2.status == Status.Ok && decodedUser == Right(User(100L, "Grace Hopper", "Pioneer"))))

        _ <- Console.printLine("\n=== 3. Requesting non-existent route for 404 ===")
        res3 <- Client.request(Request.get(URL.decode(s"$baseUri/unknown-route").toOption.get))
        _ <- Console.printLine(s"GET /unknown-route -> Status: ${res3.status} (isNotFound: ${res3.status == Status.NotFound})")
        _ <- ZIO.attempt(assert(res3.status == Status.NotFound))

        _ <- Console.printLine("\nClosing scope to trigger graceful server shutdown...")
      yield ()
    }

    program.provide(
      Server.live,
      ZLayer.succeed(Server.Config.default.port(0)),
      Client.default,
      Scope.default
    )
