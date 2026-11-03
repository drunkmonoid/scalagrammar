//> using scala 3.9.0
// Run: scala-cli run lessons/03-oop/09_export_composition.scala
//
// Export clauses (Scala 3): composition with automatic forwarders —
// "favour composition over inheritance" without boilerplate.

package oop.exports

import scala.compiletime.testing.typeCheckErrors

class Printer:
  def print(doc: String): String = s"printing '$doc'"
  def status: String = "printer ready"
  def reset(): Unit = ()

class Scanner:
  def scan(): String = "scanned page"
  def status: String = "scanner ready"

class Copier:
  private val printer = Printer()
  private val scanner = Scanner()

  export scanner.{scan, status as scannerStatus}   // forward selected members, one renamed
  export printer.{status as _, *}       // everything from printer except status (print, reset)
  def status: String = s"${printer.status} / ${scanner.status}"
  def copy(): String = print(scan())

// Exports in objects build "facades" / module APIs
object Api:
  private object impl:
    def compute(x: Int): Int = x * 2
    def internalOnly(): Unit = ()
  export impl.compute

// Exporting from a parameter: decorators / delegation
trait Repo:
  def find(id: Int): Option[String]
  def all: List[String]

class InMemoryRepo extends Repo:
  private val data = Map(1 -> "a", 2 -> "b")
  def find(id: Int) = data.get(id)
  def all = data.values.toList

// Note: LoggingRepo deliberately doesn't `extends Repo`. Export forwarders are final aliases
// and are not meant to override inherited members, so the decorator exposes the same API
// by forwarding rather than by subtyping.
class LoggingRepo(underlying: Repo):
  export underlying.all                 // forward unchanged
  def find(id: Int): Option[String] =   // decorate this one
    println(s"  find($id)")
    underlying.find(id)

// Exports of givens need `given`
object Instances:
  given Ordering[Int] = Ordering.Int.reverse
object Re:
  export Instances.given

@main def run(): Unit =
  section("Forwarders")
  val c = Copier()
  println(c.print("doc"))
  println(c.scan())
  println(c.scannerStatus)
  println(c.status)
  println(c.copy())
  c.reset()

  section("Facade objects")
  println(Api.compute(21))
  doesNotCompile("oop.exports.Api.internalOnly()")

  section("Delegation")
  val repo = LoggingRepo(InMemoryRepo())
  println(repo.find(1))
  println(repo.all)

  section("Exported givens")
  import Re.given
  println(List(1, 3, 2).sorted)          // List(3, 2, 1)

  section("Rules & corner cases")
  // * forwarders are FINAL methods with the same signature; they don't override
  //   (you can't export a member that would clash with an existing one → error)
  // * the export prefix must be a STABLE path (val, object, parameter — not a def/var)
  // * wildcard `*` does not export givens; use `given` selectors
  doesNotCompile("class D { def p = Printer(); export p.print }")   // `p` is a def
  println("Generated forwarders: " + classOf[Copier].getDeclaredMethods.map(_.getName).sorted.mkString(", "))

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
