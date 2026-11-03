//> using scala 3.9.0
// Run: scala-cli run lessons/03-oop/07_access_modifiers_packages.scala
//
// Visibility: public (default), private, protected, qualified private[X]/protected[X],
// plus packages, imports, and their many forms.

package oop.access

import scala.compiletime.testing.typeCheckErrors

package model:                       // nested package (block syntax)
  class Secretive:
    private val classPrivate = 1                 // visible in class + companion
    private[this] val instancePrivate = 2        // deprecated spelling; same as `private` in 3
    private[model] val packagePrivate = 3        // visible anywhere in package `model`
    private[access] val outerPackage = 4         // visible in oop.access and below
    protected val forSubclasses = 5
    protected[model] val subclassesOrPackage = 6
    def peek(other: Secretive) = other.classPrivate   // other instance: OK for `private`

  object Secretive:
    def reveal(s: Secretive) = s.classPrivate          // companion can see privates

  object SamePackage:
    def look(s: Secretive) = s.packagePrivate + s.subclassesOrPackage

package util:
  def helper = "top-level def inside package util"
  val constant = 42

@main def run(): Unit =
  import model.*
  val s = Secretive()

  section("private / private[pkg] / protected")
  println(s.peek(Secretive()))
  println(Secretive.reveal(s))
  println(SamePackage.look(s))
  println(s.outerPackage)                       // we're inside oop.access → visible
  doesNotCompile("oop.access.model.Secretive().classPrivate")
  doesNotCompile("oop.access.model.Secretive().forSubclasses")
  doesNotCompile("oop.access.model.Secretive().packagePrivate")

  section("Imports: every form")
  import scala.collection.mutable                // import a package, use mutable.Map
  val mm = mutable.Map(1 -> "a")
  import scala.collection.immutable.{TreeMap, TreeSet}   // several
  import java.util.{List as JList, ArrayList}   // rename one
  import scala.math.{Pi as _, *}                 // hide Pi, import the rest
  val jl: JList[Int] = ArrayList[Int]()
  println(s"$mm ${TreeMap(2 -> "b", 1 -> "a")} ${TreeSet(3, 1)} ${jl.size} ${sqrt(16)}")
  doesNotCompile("Pi")                           // hidden by `Pi as _`
  // ⚠ Scala 2 syntax `import a.{b => c, _}` still parses in Scala 3 but is deprecated.
  // ⚠ Precedence trap: a WILDCARD import in an inner scope cannot shadow an EXPLICIT import
  //   of the same name in an outer scope. `import java.util.*` here would make `TreeMap`
  //   ambiguous (java.util.TreeMap vs the explicitly imported immutable.TreeMap).

  section("Imports are scoped and can appear anywhere")
  locally {
    import util.*
    println(helper)
  }
  doesNotCompile("helper")                       // not imported in this scope

  section("Importing members of a value (not just packages/objects)")
  case class Config(host: String, port: Int)
  val cfg = Config("localhost", 8080)
  locally {
    import cfg.{host, port}                      // import from any stable identifier (a val)
    println(s"$host:$port")
  }

  section("given imports are separate")
  object Instances:
    given Ordering[String] = Ordering.String.reverse
    val plain = 1
  import Instances.*                            // does NOT import the given
  println(List("a", "c", "b").sorted)           // default ordering
  locally {
    import Instances.given                      // imports givens only
    println(List("a", "c", "b").sorted)         // reversed
  }

  section("Root imports and shadowing")
  // Every file implicitly has:  import java.lang.*; import scala.*; import Predef.*
  // Use _root_ to escape shadowed names:
  locally {
    object scala { val x = "a local thing named scala" }
    println(scala.x)                            // the local object wins
    println(_root_.scala.math.max(1, 2))        // the real package
  }

  section("Top-level definitions & package objects")
  println(util.constant)
  // Scala 2 used `package object util { def helper = ... }`. Scala 3 allows defs/vals/types
  // directly in packages; package objects are deprecated.

  section("Access checks are compile-time only")
  // Reflection can bypass them (setAccessible), and Java code sees most Scala privates as
  // public bytecode members with mangled names (e.g. qualified-private members).
  val f = classOf[Secretive].getDeclaredFields.map(_.getName).toList
  println(s"JVM fields: $f")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
