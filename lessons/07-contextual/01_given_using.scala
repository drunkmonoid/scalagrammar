//> using scala 3.9.0
// Run: scala-cli run lessons/07-contextual/01_given_using.scala
//
// Contextual parameters: `using` clauses are filled in by the compiler with `given`
// instances found in scope. (Scala 2's `implicit` parameters/values — lesson 12.)

package contextual.givens

import scala.compiletime.testing.typeCheckErrors

case class Config(appName: String, verbose: Boolean)

// A method with a context parameter
def log(msg: String)(using cfg: Config): Unit =
  if cfg.verbose then println(s"  [${cfg.appName}] $msg")

// Anonymous context parameter: name not needed if only passed along / summoned
def process(items: List[String])(using Config): Unit =
  items.foreach(i => log(s"processing $i"))       // Config passed implicitly to log
  log(s"done: ${summon[Config].appName}")          // summon = "give me the instance"

// ---------- given definitions: every form ----------
trait Greeter:
  def greet(name: String): String

object Forms:
  // 1. anonymous given with a body ("structural" given)
  given Greeter with
    def greet(name: String) = s"Hello, $name"

  // 2. named given
  given loud: Ordering[String] with
    def compare(a: String, b: String) = b.compareTo(a)

  // 3. alias given: `= expression` (evaluated lazily, cached for vals)
  given defaultConfig: Config = Config("demo", verbose = true)

  // 4. conditional given with type parameters: exists for Option[A] only if an
  //    Ordering[A] can be found
  given optionOrdering[A](using ord: Ordering[A]): Ordering[Option[A]] =
    Ordering.Option(ord)

// Scala 3.6+ "new" given syntax (equivalent; shown for recognition):
//   given Greeter:                             -- structural, `:` instead of `with`
//     def greet(name: String) = ...
//   given [A: Ordering] => Ordering[List[A]]:  -- conditional, using `=>`
//     ...
//   given Config = Config(...)                 -- alias (unchanged)

@main def run(): Unit =
  section("Passing context explicitly vs implicitly")
  val cfg = Config("explicit", verbose = true)
  log("explicit using")(using cfg)               // `using` is required at the call site
  doesNotCompile("contextual.givens.log(\"x\")")  // no given Config in scope here

  section("Bringing givens into scope")
  locally {
    given Config = Config("local", verbose = true)
    process(List("a", "b"))
  }
  locally {
    import Forms.given                           // import ALL givens of Forms
    process(List("c"))
    println(summon[Greeter].greet("Ada"))
    println(List("a", "c", "b").sorted)          // uses Forms.loud (reverse)
  }
  locally {
    import Forms.{given Config}                  // import givens BY TYPE
    log("only the Config given")
  }
  locally {
    import Forms.defaultConfig                   // or by name, like any member
    log("by name")
  }

  section("summon and its friends")
  locally {
    given Int = 42
    println(summon[Int])
    // `summon[T]` is just `def summon[T](using x: T): x.type = x`
    // `implicitly[T]` (Scala 2) still exists, but loses precise types.
    println(scala.compiletime.summonInline[Int])
  }

  section("Context bounds are sugar for using clauses")
  def maxOf[A: Ordering](xs: List[A]): A = xs.max
  // ≡ def maxOf[A](xs: List[A])(using Ordering[A]): A
  println(maxOf(List(3, 1, 2)))
  // Named context bound (3.6+): def maxOf[A: Ordering as ord](...) — then use `ord`

  section("Conditional givens chain automatically")
  locally {
    import Forms.optionOrdering
    println(List(Some(3), None, Some(1)).sorted)    // Ordering[Option[Int]] ← Ordering[Int]
    println(List(Some("b"), Some("a")).sorted)
  }

  section("Givens are lazy (alias givens) and evaluated once")
  object Lazy:
    given expensive: String =
      println("  computing given String")
      "value"
  import Lazy.given
  println("before first use")
  println(summon[String])
  println(summon[String])                         // not recomputed

  section("Using clauses can come in any parameter list, and multiple")
  def f(a: Int)(using x: Int)(b: Int)(using s: String): String = s"$a $x $b $s"
  locally {
    given Int = 1
    given String = "s"
    println(f(10)(20))
  }

  section("By-name context parameters (for recursive / lazy instances)")
  trait Show[A] { def show(a: A): String }
  given Show[Int] = _.toString
  given listShow[A](using sa: => Show[A]): Show[List[A]] = _.map(sa.show).mkString("[", ",", "]")
  println(summon[Show[List[List[Int]]]].show(List(List(1), List(2, 3))))

  section("Ambiguity is an error")
  doesNotCompile("given a: Int = 1; given b: Int = 2; summon[Int]")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
