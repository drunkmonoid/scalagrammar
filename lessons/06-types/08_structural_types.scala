//> using scala 3.9.0
// Run: scala-cli run lessons/06-types/08_structural_types.scala
//
// Structural types: "any value that HAS these members", regardless of its class.
// Scala 3 implements member access on structural types through `Selectable`,
// which lets you back them with reflection, maps, databases, etc.

package types.structural

import scala.reflect.Selectable.reflectiveSelectable   // enables reflection-based access
import scala.compiletime.testing.typeCheckErrors

// A structural type alias
type Closeable = { def close(): Unit }

class FileHandle:
  def close(): Unit = println("  file closed")
class SocketHandle:
  def close(): Unit = println("  socket closed")

def withResource[R <: Closeable, A](r: R)(f: R => A): A =
  try f(r) finally r.close()        // reflective call (slow-ish, but works on any class)

// ---------- Selectable backed by a Map: typed records ----------
class Record(fields: (String, Any)*) extends Selectable:
  private val m = fields.toMap
  def selectDynamic(name: String): Any = m(name)

type Person = Record { val name: String; val age: Int }

// ---------- refinement of a normal type ----------
trait Animal:
  def name: String
type NamedAndAged = Animal { def age: Int }

@main def run(): Unit =
  section("Duck typing via reflection")
  withResource(FileHandle())(_ => println("  using file"))
  withResource(SocketHandle())(_ => println("  using socket"))
  val adHoc: { def close(): Unit } = new { def close() = println("  anonymous closed") }
  adHoc.close()

  section("Selectable records (no reflection)")
  val p = Record("name" -> "Ada", "age" -> 36).asInstanceOf[Person]
  println(s"${p.name} is ${p.age}")        // p.name → p.selectDynamic("name").asInstanceOf[String]
  val upper: String = p.name.toUpperCase   // statically typed!
  println(upper)
  doesNotCompile("val p = types.structural.Record().asInstanceOf[types.structural.Person]; p.email")

  section("Refinements on normal types")
  val a: NamedAndAged = new Animal { def name = "Rex"; def age = 3 }
  println(s"${a.name} ${a.age}")

  section("Without the import, reflective access is a feature error")
  // Delete the `reflectiveSelectable` import at the top and the `withResource` method fails with:
  //   "value close is not a member of R ... Selectable"
  println("see comment")

  section("Performance note")
  // reflectiveSelectable uses java.lang.reflect on every call (cached per call site in Scala 2,
  // not guaranteed in 3). Prefer traits/type classes in hot paths; structural types for
  // ad-hoc glue, and Selectable for typed views of dynamic data (DB rows, JSON).
  println("ok")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
