//> using scala 3.9.0
// See also: lessons/06-types/08_structural_types.scala
// Run: scala-cli run lessons/11-advanced/09_dynamic.scala
//
// scala.Dynamic: runtime resolution of arbitrary member selections, calls, and assignments.

package advanced.dynamic

import scala.language.dynamics
import scala.compiletime.testing.typeCheckErrors

class FlexibleDynamic extends Dynamic:
  var lastAssignment: Option[(String, Any)] = None

  def selectDynamic(name: String): String =
    name

  def applyDynamic(name: String)(args: Any*): String =
    s"$name(${args.mkString(", ")})"

  def updateDynamic(name: String)(value: Any): Unit =
    lastAssignment = Some((name, value))

@main def run(): Unit =
  section("Dynamic field selection")
  val dyn = FlexibleDynamic()
  val selected = dyn.anyFieldName
  println(selected) // anyFieldName

  section("Dynamic method invocation")
  val called = dyn.calculate(1, 2, "test")
  println(called) // calculate(1, 2, test)

  section("Dynamic assignment")
  dyn.targetField = 42
  println(dyn.lastAssignment) // Some((targetField,42))

  section("Selectable vs Dynamic")
  // `Selectable` from lessons/06-types/08_structural_types.scala is the better tool when:
  //   1. The member set is known at compile time via a refinement (e.g. `Record { val name: String }`).
  //   2. The result type should not be `Any` (Selectable preserves precise statically-checked types).
  //   3. Typos in member names should be caught at compile time.
  // `Dynamic` is preferred for dynamic data models (e.g. untyped JSON, arbitrary RPC facades).
  println("see comments")

  section("Compile-time safety")
  // Invoking dynamic members on a non-Dynamic type must fail:
  doesNotCompile("""
    class Plain
    val p = Plain()
    p.missingMember
  """)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
