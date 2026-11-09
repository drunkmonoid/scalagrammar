//> using scala 3.9.0
//> using dep com.lihaoyi::cask:0.10.2
//> using dep com.lihaoyi::requests:0.9.3
//> using dep com.lihaoyi::upickle:4.4.3
// See also: ecosystem/lihaoyi/01_os_lib_requests.scala
// Run: scala-cli run ecosystem/lihaoyi/04_cask.scala
//
// Web services with Cask and requests:
// 1. Cask routes with HTTP annotations (@cask.get, @cask.post, and JSON responses)
// 2. Dynamic ephemeral port allocation and Undertow server lifecycle
// 3. In-process HTTP testing with Li Haoyi's requests library
// 4. Clean server shutdown ensuring process terminates with exit 0

package ecosystem.lihaoyi.caskdemo

import cask.*
import io.undertow.Undertow
import java.net.ServerSocket

// Cask Routes definition
object CaskRoutes extends cask.Routes:
  @cask.get("/hello")
  def hello(): String = "Hello from Cask!"

  @cask.post("/echo")
  def echo(request: cask.Request): String =
    val body = request.text()
    s"Echoed: $body"

  @cask.get("/json")
  def jsonEndpoint(): ujson.Obj =
    ujson.Obj(
      "status" -> 200,
      "framework" -> "cask",
      "author" -> "Li Haoyi"
    )

  initialize()

def findFreePort(): Int =
  val socket = ServerSocket(0)
  try socket.getLocalPort
  finally socket.close()

@main def run(): Unit =
  section("1. Allocating dynamic port and starting Cask server")
  val port = findFreePort()
  val host = "localhost"

  val server = Undertow.builder()
    .addHttpListener(port, host)
    .setHandler(CaskRoutes.defaultHandler)
    .build()

  server.start()
  println(s"Cask server listening on http://$host:$port")

  val baseUrl = s"http://$host:$port"

  try
    section("2. Invoking @cask.get endpoint via requests")
    val getRes = requests.get(s"$baseUrl/hello")
    println(s"GET /hello -> Status: ${getRes.statusCode}, Body: '${getRes.text()}'")
    assert(getRes.statusCode == 200)
    assert(getRes.text() == "Hello from Cask!")

    section("3. Invoking @cask.post endpoint via requests")
    val postPayload = "Scala 3 metaprogramming and web services"
    val postRes = requests.post(s"$baseUrl/echo", data = postPayload)
    println(s"POST /echo -> Status: ${postRes.statusCode}, Body: '${postRes.text()}'")
    assert(postRes.statusCode == 200)
    assert(postRes.text() == s"Echoed: $postPayload")

    section("4. Invoking JSON endpoint via requests and ujson parsing")
    val jsonRes = requests.get(s"$baseUrl/json")
    println(s"GET /json -> Status: ${jsonRes.statusCode}, Body: '${jsonRes.text()}'")
    assert(jsonRes.statusCode == 200)
    val parsed = ujson.read(jsonRes.text())
    assert(parsed("status").num == 200)
    assert(parsed("framework").str == "cask")
    assert(parsed("author").str == "Li Haoyi")
  finally
    section("5. Stopping Cask Undertow server")
    server.stop()
    println("Cask server stopped cleanly.")

def section(title: String): Unit = println(s"\n=== $title ===")
