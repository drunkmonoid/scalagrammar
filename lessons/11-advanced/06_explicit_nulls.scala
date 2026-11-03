//> using scala 3.9.0
//> using options -Yexplicit-nulls
// Run: scala-cli run lessons/11-advanced/06_explicit_nulls.scala
//
// Explicit nulls (opt-in compiler flag): `null` is no longer a member of every reference type.
//   String          — can never be null
//   String | Null   — might be null; you must check before use
// Java APIs are seen with nullable types where they may return null (with some
// heuristics), so the compiler forces you to handle them.

package advanced.explicitnulls

import scala.compiletime.testing.typeCheckErrors

def lengthOf(s: String | Null): Int =
  if s == null then 0
  else s.length            // flow typing: s is String here

case class Node(value: Int, var next: Node | Null)

@main def run(): Unit =
  section("Null is no longer a subtype of String")
  println(typeCheckErrors("val s: String = null").map(_.message.linesIterator.next()))
  val maybe: String | Null = null
  println(typeCheckErrors("val m: String | Null = null; m.length").map(_.message.linesIterator.next()))

  section("Flow typing narrows after a null check")
  println(lengthOf(null) -> lengthOf("abc"))
  val v: String | Null = System.getProperty("user.home")
  if v != null then println(s"  home has ${v.length} chars")   // v: String inside

  section(".nn: assert non-null (throws NullPointerException if wrong)")
  val home: String = System.getProperty("user.home").nn
  println(home.nonEmpty)
  try
    val missing: String = System.getProperty("no.such.property").nn
    println(missing)
  catch case e: NullPointerException => println("  .nn on null → NullPointerException")

  section("Java interop")
  // System.getProperty is typed as returning String | Null under explicit nulls:
  val prop = System.getProperty("java.version")
  val len = if prop == null then -1 else prop.length
  println(len)
  // Wrapping into Option is the usual boundary:
  println(Option(System.getenv("NOT_SET_ANYWHERE")))

  section("Mutable fields: no flow typing for vars of other objects")
  val n = Node(1, Node(2, null))
  val next = n.next                     // copy to a local val …
  if next != null then println(s"  next value: ${next.value}")   // … then it narrows
  // `if n.next != null then n.next.value` doesn't compile: n.next is a var and could change.
  println(typeCheckErrors(
    "val n = advanced.explicitnulls.Node(1, null); if n.next != null then n.next.value else 0"
  ).map(_.message.linesIterator.next()))

  section("Why opt in?")
  // NullPointerExceptions become compile errors. Costs: annotating Java boundaries, and
  // libraries compiled without the flag are treated leniently. Many Scala 3 codebases
  // simply use Option and never null, and enable the flag for extra safety.
  println(maybe == null)

def section(title: String): Unit = println(s"\n=== $title ===")
