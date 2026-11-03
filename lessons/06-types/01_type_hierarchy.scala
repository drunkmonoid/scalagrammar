//> using scala 3.9.0
// Run: scala-cli run lessons/06-types/01_type_hierarchy.scala
//
//                              Any
//                    ┌──────────┴──────────┐
//                Matchable           (universal traits, opaque types)
//              ┌─────┴─────────┐
//           AnyVal           AnyRef (java.lang.Object)
//   Int Long Double ...       String, List, your classes, Java classes...
//   Boolean Char Unit                    │
//   value classes                       Null   (only with explicit nulls OFF)
//              └───────────┬─────────────┘
//                       Nothing
//
// Any       — top type; has ==, !=, equals, hashCode, toString, ##, isInstanceOf, asInstanceOf
// Matchable — values you may pattern match on (Scala 3)
// AnyVal    — value types; no `null`, no `eq`
// AnyRef    — reference types; has eq/ne/synchronized/wait/notify
// Null      — type of `null`; subtype of every AnyRef (unless -Yexplicit-nulls)
// Nothing   — bottom; no values. Type of throw, ???, sys.exit, empty collections' elements

package types.hierarchy

import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("Everything is an Any")
  val things: List[Any] = List(1, "two", 3.0, '4', true, (), null, List(5), () => 6)
  things.foreach(t => println(f"  ${String.valueOf(t)}%-28s ${if t == null then "null" else t.getClass.getName}"))

  section("Unit has one value: ()")
  val u: Unit = ()
  println(u == ())
  println((u: Any).getClass)        // ⚠ class scala.runtime.BoxedUnit when boxed

  section("Nothing: the bottom type")
  def fail(): Nothing = throw RuntimeException("nope")
  val i: Int = if true then 1 else fail()      // Nothing <: Int
  val s: String = if true then "ok" else ???   // ??? : Nothing
  println(i -> s)
  val empty: List[Nothing] = Nil                // Nil is List[Nothing]
  val ints: List[Int] = empty                   // covariance + Nothing ⇒ fits anywhere
  println(ints)
  // An expression of type Nothing never completes normally, so code after it is dead.

  section("Null")
  val str: String = null                         // allowed: Null <: String
  doesNotCompile("val n: Int = null")            // Null is NOT a subtype of AnyVal
  println(str == null)
  // Option(null) is None; Some(null) is Some(null) ⚠
  println(Option(str) -> Some(str))
  // With -Yexplicit-nulls (see lessons/11-advanced), `String` excludes null and you write
  // `String | Null`, using `.nn` to assert non-null.

  section("AnyVal vs AnyRef capabilities")
  doesNotCompile("1 eq 1")
  println(1.synchronized { "compiles, with a warning: Suspicious synchronized call on boxed class" })
  println("abc".eq("abc"))
  println(classOf[Int])                          // int
  println(classOf[java.lang.Integer])
  println(classOf[String])

  section("Matchable (Scala 3)")
  // A value of an unconstrained type parameter might be an opaque alias whose runtime
  // representation would leak through a match. Matching on Any/unbounded T warns under
  // -source future. Matchable is the bound that permits it.
  def safe[T <: Matchable](t: T) = t match
    case _: Int => "Int"
    case _      => "other"
  println(safe(1))

  section("Top-level type inference: widening")
  val x = if true then 1 else "a"               // Matchable (join) — see 01-basics/02
  val y = List(1, "a")                          // List[Matchable]
  println(y)
  // Scala 3 dropped "weak conformance". Only numeric LITERALS are harmonised to a common
  // numeric type; non-literal values are not widened:
  val z = List(1, 2L)                           // List[Long] — the literal 1 becomes 1L
  println(z.map(_.getClass.getSimpleName))
  val w = List(1, 2.5)                          // List[Double]
  println(w)
  val one = 1
  val mixed = List(one, 2.5)                    // ⚠ List[AnyVal] in Scala 3 (List[Double] in Scala 2)
  println(mixed.map(_.getClass.getSimpleName))

  section("isInstanceOf / asInstanceOf")
  val any: Any = "hello"
  println(any.isInstanceOf[String])
  println(any.asInstanceOf[String].length)
  println((1: Any).isInstanceOf[Int])           // true (boxed Integer matches Int)
  println((1: Any).isInstanceOf[Long])          // false
  println(null.isInstanceOf[String])            // ⚠ false
  println(null.asInstanceOf[String])            // null — cast of null always succeeds
  println(null.asInstanceOf[Int])               // ⚠ 0 — unboxing null gives the default value!
  try (any.asInstanceOf[Integer]: Integer)
  catch case e: ClassCastException => println("  ClassCastException")
  // ⚠ casts to generic types are unchecked:
  val sneaky = List("a").asInstanceOf[List[Int]]   // no error here...
  try println(sneaky.head + 1)                       // ...ClassCastException later
  catch case e: ClassCastException => println("  delayed ClassCastException")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
