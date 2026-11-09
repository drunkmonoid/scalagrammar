//> using scala 3.9.0
//> using dep org.http4s::http4s-ember-server:0.23.38
//> using dep org.http4s::http4s-ember-client:0.23.38
//> using dep org.http4s::http4s-dsl:0.23.38
//> using dep org.slf4j:slf4j-simple:2.0.20
// See also: ecosystem/http/01_http4s.scala
// Run: scala-cli run ecosystem/http/03_http4s_middleware.scala
//
// http4s authentication middleware with Ember server and client.
// Middleware inspects HTTP headers and short-circuits with 401 Unauthorized
// or passes the request to underlying routes.

package ecosystem.http.authmiddleware

import cats.data.{Kleisli, OptionT}
import cats.effect.*
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.typelevel.ci.CIString

val tokenHeaderName: CIString = CIString("X-Token")

def authMiddleware(routes: HttpRoutes[IO]): HttpRoutes[IO] = Kleisli { req =>
  val token = req.headers.get(tokenHeaderName).map(_.head.value)
  if token.contains("ok") then
    routes(req)
  else
    OptionT.pure[IO](Response[IO](Status.Unauthorized).withEntity("unauthorized"))
}

val pingRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
  case GET -> Root / "ping" =>
    Ok("pong")
}

@main def run(): Unit =
  section("http4s auth middleware on ephemeral port")
  val httpApp = authMiddleware(pingRoute).orNotFound

  val app = for
    server <- EmberServerBuilder.default[IO]
                .withHost(host"127.0.0.1")
                .withPort(port"0")
                .withHttpApp(httpApp)
                .build
    client <- EmberClientBuilder.default[IO].build
  yield (server.baseUri, client)

  app.use { (base, client) =>
    for
      // 1. Request without header -> 401 Unauthorized
      unauthResp <- client.run(Request[IO](Method.GET, base / "ping")).use { resp =>
        resp.bodyText.compile.string.map(body => (resp.status.code, body))
      }
      _ <- IO.println(s"Status: ${unauthResp._1}, Body: ${unauthResp._2}") // Status: 401, Body: unauthorized

      // 2. Request with X-Token: ok -> 200 OK
      authReq = Request[IO](Method.GET, base / "ping")
                  .putHeaders(Header.Raw(tokenHeaderName, "ok"))
      authResp <- client.run(authReq).use { resp =>
        resp.bodyText.compile.string.map(body => (resp.status.code, body))
      }
      _ <- IO.println(s"Status: ${authResp._1}, Body: ${authResp._2}") // Status: 200, Body: pong
    yield ()
  }.unsafeRunSync()

def section(title: String): Unit = println(s"\n=== $title ===")
