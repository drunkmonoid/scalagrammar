//> using scala 3.9.0
//> using platform js
//> using jsModuleKind common
// Run: scala-cli run ecosystem/platforms/01_scala_js.scala        (needs Node.js)
//      scala-cli --power package ecosystem/platforms/01_scala_js.scala -o out.js   (emit JavaScript)
//
// Scala.js compiles Scala to JavaScript: the same language, most of the standard library,
// and many libraries (cats, circe, ZIO, ...) cross-published with the `%%%` / `::` suffix.
// Here it runs on Node.js; in a browser you'd typically use a bundler (Vite + scalajs plugin).

package ecosystem.platforms.js

import scala.scalajs.js
import scala.scalajs.js.annotation.*

// ---------- facades: typed views of JavaScript APIs ----------
@js.native
@JSGlobal("Math")
object JSMath extends js.Object:
  def random(): Double = js.native
  def hypot(xs: Double*): Double = js.native

@js.native
@JSImport("os", JSImport.Namespace)     // a Node.js module
object NodeOs extends js.Object:
  def platform(): String = js.native
  def cpus(): js.Array[js.Object] = js.native

// ---------- exporting Scala to JavaScript ----------
@JSExportTopLevel("greet")
def greet(name: String): String = s"Hello from Scala.js, $name!"

case class Point(x: Double, y: Double)

@main def run(): Unit =
  println("=== plain Scala runs unchanged ===")
  val pts = List(Point(3, 4), Point(1, 1))
  println(pts.map(p => math.hypot(p.x, p.y)))    // prints 5, not 5.0: Doubles print like JS numbers
  println(pts.sortBy(_.x).mkString(", "))

  println("\n=== calling JavaScript through facades ===")
  println(s"  JS Math.hypot(3, 4) = ${JSMath.hypot(3, 4)}")
  println(s"  running on: ${NodeOs.platform()} with ${NodeOs.cpus().length} cpus")

  println("\n=== dynamic JavaScript (untyped) ===")
  val obj = js.Dynamic.literal(name = "Ada", langs = js.Array("scala", "js"))
  println(s"  ${js.JSON.stringify(obj)}")
  // ⚠ `js.Date()` would call the companion's apply, which mirrors JavaScript's `Date()` called
  //   as a FUNCTION — it returns a String! Use `new` to construct a JS object:
  println(s"  js.Date() is a ${js.typeOf(js.Date())}")
  val date = new js.Date()
  println(s"  new js.Date() year: ${date.getFullYear()}")
  val jsArr = js.Array(3, 1, 2)
  jsArr.push(0)
  println(s"  js.Array sorted: ${jsArr.toList.sorted}")   // convert to Scala collections

  println("\n=== semantic differences from the JVM ===")
  // Numbers: Int arithmetic is emulated correctly (overflow wraps like the JVM)…
  println(s"  Int.MaxValue + 1 = ${Int.MaxValue + 1}")
  // …but Char/Int/Double share JS numbers at runtime, so type tests can be surprising:
  println(s"  (1.0: Any).isInstanceOf[Int] = ${(1.0: Any).isInstanceOf[Int]}")   // true on JS!
  println(s"  1.0.toString = ${1.0.toString}")                                   // "1" on JS, "1.0" on JVM
  // No threads: concurrency is single-threaded (Futures run on the JS event loop).
  // No reflection, and java.io/java.nio are only partially available.
