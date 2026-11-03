//> using scala 3.9.0
// Run: scala-cli run lessons/02-functions/01_methods.scala
//
// Methods (`def`) — parameters, defaults, named args, varargs, multiple parameter
// lists, overloading, type parameters, and the corner cases of each.

package functions.methods

import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("Basic definitions")
  def add(a: Int, b: Int): Int = a + b
  def greet(name: String) = s"hi $name"  // result type inferred (always annotate public API!)
  def unitMethod(): Unit = println("  side effect")
  println(add(1, 2))
  println(greet("Ada"))
  unitMethod()
  // ⚠ Recursive methods MUST declare their result type:
  doesNotCompile("def fact(n: Int) = if n <= 1 then 1 else n * fact(n - 1)")

  section("Default and named arguments")
  def box(content: String, width: Int = 10, fill: Char = '.'): String =
    content.padTo(width, fill)
  println(box("a"))
  println(box("a", 5))
  println(box("a", fill = '#'))          // skip width by naming
  println(box(fill = '-', content = "b")) // any order when named
  // Defaults may refer to parameters of EARLIER parameter lists…
  def range(from: Int)(to: Int = from + 3) = from to to
  println(range(5)())
  // ⚠ …but not to other parameters of the SAME list:
  doesNotCompile("def r(from: Int, to: Int = from + 3) = to")
  // ⚠ Defaults are evaluated at EACH call, not once (unlike Python):
  def stamp(xs: List[Long] = List(System.nanoTime())) = xs.head
  println(stamp() != stamp())            // true
  // ⚠ positional after named is not allowed unless in position:
  // (typeCheckErrors can't see local definitions, so the snippet defines its own `box`)
  doesNotCompile("""def box(content: String, width: Int = 10) = content; box(width = 3, "x")""")

  section("Varargs (repeated parameters)")
  def sum(xs: Int*): Int = xs.sum        // xs: Seq[Int]
  println(sum())
  println(sum(1, 2, 3))
  val nums = List(4, 5, 6)
  println(sum(nums*))                    // splice a Seq (Scala 2: nums: _*)
  def fmt(prefix: String, items: Any*) = prefix + items.mkString(",")
  println(fmt("items:", 1, "a", true))
  // ⚠ varargs must be last; only one per parameter list:
  doesNotCompile("def bad(xs: Int*, y: Int) = 0")

  section("Multiple parameter lists")
  def mul(a: Int)(b: Int): Int = a * b
  println(mul(3)(4))
  val triple = mul(3)                    // partially applied → Int => Int (auto eta-expansion)
  println(triple(5))
  // Use #1: type inference flows left to right across lists
  def foldL[A, B](xs: List[A])(z: B)(f: (B, A) => B): B = xs.foldLeft(z)(f)
  println(foldL(List(1, 2, 3))(0)(_ + _)) // A and B known before the lambda is typed
  // Use #2: block-like syntax for the last argument
  def repeat(n: Int)(body: => Unit): Unit = for _ <- 1 to n do body
  repeat(2) {
    println("  in a block")
  }
  // Use #3: implicit/using parameters go in their own list (see lesson 07)

  section("Overloading")
  // ⚠ local defs CANNOT be overloaded, so the overloads live in `object Over` below.
  doesNotCompile("def h(i: Int) = 1; def h(s: String) = 2")
  println(Over.show(1))
  println(Over.show(1L))                 // ⚠ picks Double! (Long → Double widening; no Long overload)
  println(Over.show('c'))                // ⚠ Int (Char → Int widening)
  println(Over.show("s"))
  // ⚠ overloads that differ only by erased type arguments clash:
  doesNotCompile("object O { def f(xs: List[Int]) = 1; def f(xs: List[String]) = 2 }")
  // ⚠ default args are allowed on at most ONE overloaded alternative

  section("Type parameters")
  def first[A](xs: List[A]): A = xs.head
  println(first(List("x", "y")))
  println(first[Int](List(1)))           // explicit type argument
  def pair[A, B](a: A, b: B): (A, B) = (a, b)
  println(pair(1, "one"))
  // Clause interleaving: type params can appear between value params (Scala 3.6+)
  def getOrElseFor[K](m: Map[K, Int])(key: K)[V >: Int](default: V): V = m.getOrElse(key, default)
  println(getOrElseFor(Map("a" -> 1))("b")("none"))

  section("Procedure syntax is gone; result types")
  doesNotCompile("def p() { println(1) }")   // Scala 2 procedure syntax — error in Scala 3
  // Methods with `: Unit = ` are the replacement.

  section("Nested (local) methods and closures over locals")
  def outer(n: Int): Int =
    def helper(k: Int): Int = k * n      // captures n
    helper(2) + helper(3)
  println(outer(10))

  section("Methods vs functions")
  // A method is not a value; it becomes one through eta-expansion (automatic in Scala 3).
  val f: (Int, Int) => Int = add         // eta-expansion: (a, b) => add(a, b)
  val g = add                            // Scala 3: automatic, g: (Int, Int) => Int
  println(f(1, 2) + g(3, 4))
  // ⚠ methods with no parameter list are evaluated, not eta-expanded:
  def answer: Int = 42
  val v = answer                         // Int, not () => Int
  println(v)

  section("Evaluation order of arguments")
  def trace(tag: String, v: Int) = { print(s"$tag "); v }
  println(add(trace("first", 1), trace("second", 2)))   // left to right, always
  println(box(fill = { print("fill "); '*' }, content = { print("content "); "x" }))
  // ⚠ named args are evaluated in the order WRITTEN, not the parameter order.

  section("Extension of `this`: methods on objects/classes")
  println(Circle(2).area)
  println(Circle(2).scaled(2).area)

object Over:
  def show(i: Int): String = s"Int $i"
  def show(s: String): String = s"String $s"
  def show(d: Double): String = s"Double $d"

case class Circle(r: Double):
  def area: Double = math.Pi * r * r
  def scaled(k: Double): Circle = copy(r = r * k)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
