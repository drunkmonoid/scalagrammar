//> using scala 3.9.0
// Run: scala-cli run lessons/07-contextual/06_resolution_rules.scala
//
// Where does the compiler look for a given, and which one wins?
//
// 1. LEXICAL scope first: local givens, givens of enclosing classes/objects, givens imported
//    with `import x.given` / `import x.{given T}`. Inner (more deeply nested) scopes win.
// 2. If nothing is found lexically: the IMPLICIT SCOPE of the type being searched, i.e.
//    the companion objects of every class/trait "part" of the type: for Show[List[User]]
//    that is Show, List, User (and their supertypes' companions), plus opaque-type companions.
// 3. Among several candidates at the same level, the most specific one wins; otherwise the
//    search is ambiguous (compile error).
// 4. Priority trick: put low-priority givens in a parent trait of the companion.

package contextual.resolution

import scala.compiletime.testing.typeCheckErrors

trait Show[A]:
  def show(a: A): String

// Instances placed in different "parts" of the implicit scope
case class User(name: String)
object User:
  given Show[User] = u => s"User(${u.name}) [from User companion]"

object Show:
  given Show[Int] = i => s"$i [from Show companion]"
  // Low priority fallback for ANY type, via inheritance from a trait:
  given [A]: Show[List[A]] = l => s"list of ${l.size} [generic, Show companion]"

// Prioritisation through inheritance: of two otherwise-equal candidates, the one defined in
// a SUBCLASS wins over the one inherited from the parent trait. Libraries use this to layer
// instances (e.g. cats' LowPriorityInstances traits).
trait LowPriority:
  given lowInt: Pretty[Int] = i => s"low-priority int $i"
trait Pretty[A]:
  def pretty(a: A): String
object Pretty extends LowPriority:
  given highInt: Pretty[Int] = i => s"high-priority int $i"   // same type, defined in subclass → wins
  given Pretty[String] = s => s"string $s"

def show[A](a: A)(using s: Show[A]): String = s.show(a)
def pretty[A](a: A)(using p: Pretty[A]): String = p.pretty(a)

@main def run(): Unit =
  section("Implicit scope: companions of the type's parts")
  println(show(User("ada")))            // found in User's companion
  println(show(42))                     // found in Show's companion
  println(show(List(1, 2)))             // Show companion, generic list instance

  section("Lexical scope beats implicit scope")
  locally {
    given Show[Int] = i => s"$i [local]"
    println(show(42))
  }

  section("Nesting: inner wins")
  locally {
    given Show[Int] = _ => "outer"
    locally {
      given Show[Int] = _ => "inner"
      println(show(1))
    }
  }

  section("Same level, two candidates → ambiguity")
  doesNotCompile("""
    given a: contextual.resolution.Show[Int] = _ => "a"
    given b: contextual.resolution.Show[Int] = _ => "b"
    contextual.resolution.show(1)
  """)

  section("Priority by inheritance (the LowPriority pattern)")
  println(pretty(1))                    // highInt, not the inherited lowInt
  println(pretty("x"))
  // ⚠ When candidates differ in TYPE specificity too (e.g. Monad[F] vs Functor[F], or a
  //   generic [A] instance vs a specific one), the rules are subtler and changed in Scala 3.7.
  //   Keep instances for the same type unambiguous instead of relying on fine print.

  section("Explicit is always possible")
  println(show(1)(using _ => "hand-made"))

  section("Imports: wildcard vs given")
  object Instances:
    given Show[String] = s => s"'$s'"
    val notGiven = 0
  locally {
    import Instances.*                  // ⚠ does NOT import givens in Scala 3
    doesNotCompile("contextual.resolution.show(\"x\")")
  }
  locally {
    import Instances.given
    println(show("x"))
  }

  section("Debugging resolution")
  // * summon[T] at a point shows what is in scope
  // * scalac -Xprint:typer shows the inserted arguments:
  //     scala-cli compile file.scala -O -Xprint:typer
  // * Errors explain which candidates were tried and why they failed.
  println("see comments")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `${code.trim.linesIterator.map(_.trim).mkString("; ")}`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
