//> using scala 3.9.0
//> using dep org.apache.pekko::pekko-actor-typed:1.7.1
//> using dep org.apache.pekko::pekko-stream:1.7.1
//> using dep org.apache.pekko::pekko-http:1.1.0
//> using dep org.slf4j:slf4j-nop:2.0.20
// See also: ecosystem/actors/01_pekko_typed.scala
// Run: scala-cli run ecosystem/actors/02_pekko_streams_http.scala
//
// Apache Pekko Streams and Pekko HTTP:
// 1. Reactive streams with Source, Flow, Sink, rate throttling, concurrent mapAsync, and chunk grouping
// 2. Pekko HTTP routing with port 0 binding, JSON responses, and HTTP client consumption
// 3. Clean server unbinding and ActorSystem lifecycle termination

package ecosystem.actors.pekkohttp

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.scaladsl.{Source, Flow, Sink}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.util.ByteString
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*

@main def run(): Unit =
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "PekkoStreamsHttpDemo")
  implicit val ec: scala.concurrent.ExecutionContext = system.executionContext

  try
    section("1. Pekko Streams: throttle, mapAsync, grouped")
    // Build a stream that processes items concurrently and groups results
    val streamFuture = Source(1 to 10)
      .throttle(50, 1.second)
      .via(Flow[Int].mapAsync(parallelism = 4)(x => Future(x * 10)))
      .grouped(5)
      .runWith(Sink.seq)

    val materialized = Await.result(streamFuture, 5.seconds)
    println(s"Materialized stream groups: $materialized")
    assert(materialized == Seq(Seq(10, 20, 30, 40, 50), Seq(60, 70, 80, 90, 100)))

    section("2. Pekko HTTP server & client on dynamic port 0")
    val route = path("api" / "status") {
      get {
        complete(
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              """{"status":"healthy","engine":"pekko-http"}"""
            )
          )
        )
      }
    }

    // Bind server to an ephemeral port
    val binding = Await.result(Http().newServerAt("127.0.0.1", 0).bind(route), 5.seconds)
    val port = binding.localAddress.getPort
    println(s"Pekko HTTP server bound on http://127.0.0.1:$port")

    // Client request using singleRequest:
    val requestUri = s"http://127.0.0.1:$port/api/status"
    val response = Await.result(Http().singleRequest(HttpRequest(uri = requestUri)), 5.seconds)
    val responseBody = Await.result(
      response.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(_.utf8String),
      5.seconds
    )

    println(s"Client received status: ${response.status}")
    println(s"Client received body:   $responseBody")
    assert(response.status == StatusCodes.OK)
    assert(responseBody.contains("\"status\":\"healthy\""))

    section("3. Clean shutdown")
    Await.result(binding.unbind(), 5.seconds)
    println("HTTP server port unbound successfully.")
  finally
    system.terminate()
    Await.result(system.whenTerminated, 5.seconds)
    println("ActorSystem terminated cleanly.")

def section(title: String): Unit = println(s"\n=== $title ===")
