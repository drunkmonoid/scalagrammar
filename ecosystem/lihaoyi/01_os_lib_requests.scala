//> using scala 3.9.0
//> using dep com.lihaoyi::os-lib:0.11.10
//> using dep com.lihaoyi::requests:0.9.3
//> using dep com.lihaoyi::upickle:4.4.3
// Run: scala-cli run ecosystem/lihaoyi/01_os_lib_requests.scala
//
// The "Li Haoyi stack": small, Python-like libraries that make Scala great for scripting.
//   os-lib    — files, paths and subprocesses
//   requests  — a blocking HTTP client modelled on Python's requests
//   upickle   — JSON (see ecosystem/json)
// Also worth knowing: mainargs (CLI parsing), cask (web server), scalatags (HTML), pprint,
// sourcecode, fansi, and Mill (the build tool).

package ecosystem.lihaoyi.oslib

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

@main def run(): Unit =
  println("=== os-lib: paths ===")
  val tmp = os.temp.dir(prefix = "scalagrammar-")      // auto-deleted on JVM exit
  val notes = tmp / "notes" / "today.txt"               // `/` builds paths; Path is always absolute
  println(s"  ${notes.relativeTo(tmp)}  name=${notes.last}  ext=${notes.ext}  parent=${notes / os.up}")
  val rel = os.RelPath("a/b/../c")                       // relative paths are a separate type
  println(s"  normalised relative path: $rel")

  println("\n=== os-lib: files ===")
  os.write(notes, "first line\n", createFolders = true)
  os.write.append(notes, "second line\n")
  println(s"  read: ${os.read.lines(notes)}")
  os.copy(notes, tmp / "backup.txt")
  os.move(tmp / "backup.txt", tmp / "notes" / "backup.txt")
  os.makeDir.all(tmp / "deep" / "nested" / "dir")
  println(s"  list:  ${os.list(tmp / "notes").map(_.last)}")
  println(s"  walk:  ${os.walk(tmp).map(_.relativeTo(tmp).toString).sorted}")
  println(s"  stat:  size=${os.size(notes)} isFile=${os.isFile(notes)} exists=${os.exists(tmp / "nope")}")
  os.remove.all(tmp / "deep")

  println("\n=== os-lib: subprocesses ===")
  val res = os.proc("echo", "hello from a subprocess").call()
  println(s"  exit=${res.exitCode} out=${res.out.trim()}")
  val ls = os.proc("ls", "-1").call(cwd = tmp / "notes")
  println(s"  ls -1 in notes/: ${ls.out.lines()}")
  val failed = os.proc("ls", "/definitely/missing").call(check = false, stderr = os.Pipe)
  println(s"  failing command: exit=${failed.exitCode}")
  // pipelines: feed stdin, read stdout
  val sorted = os.proc("sort").call(stdin = "pear\napple\nfig\n")
  println(s"  sort via stdin: ${sorted.out.lines()}")

  println("\n=== requests: HTTP client (against a local JDK server) ===")
  val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
  server.createContext("/echo", ex => {
    val body = String(ex.getRequestBody.readAllBytes())
    val reply = ujson.Obj("method" -> ex.getRequestMethod, "query" -> Option(ex.getRequestURI.getQuery).getOrElse(""), "body" -> body)
    val bytes = ujson.write(reply).getBytes
    ex.getResponseHeaders.add("Content-Type", "application/json")
    ex.sendResponseHeaders(200, bytes.length)
    ex.getResponseBody.write(bytes); ex.close()
  })
  server.createContext("/missing", ex => { ex.sendResponseHeaders(404, -1); ex.close() })
  server.start()
  val base = s"http://127.0.0.1:${server.getAddress.getPort}"
  try
    val r = requests.get(s"$base/echo", params = Map("q" -> "scala", "page" -> "2"))
    println(s"  GET  → ${r.statusCode} ${r.text()}")
    val p = requests.post(s"$base/echo", data = ujson.write(ujson.Obj("name" -> "Ada")),
                          headers = Map("Content-Type" -> "application/json"))
    println(s"  POST → ${ujson.read(p.text())("body").str}")
    val missing = requests.get(s"$base/missing", check = false)    // don't throw on 4xx/5xx
    println(s"  404 with check=false → ${missing.statusCode}")
    try requests.get(s"$base/missing")
    catch case e: requests.RequestFailedException => println(s"  404 with check=true → ${e.getClass.getSimpleName}")
  finally server.stop(0)
