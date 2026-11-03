//> using scala 3.9.0
// Run: scala-cli run lessons/11-advanced/01_for_comprehensions.scala
//
// for-comprehensions are SYNTAX SUGAR. Any type with the right methods works:
//
//   for x <- e yield f(x)                     ⇒ e.map(x => f(x))
//   for x <- e1; y <- e2 yield f(x, y)        ⇒ e1.flatMap(x => e2.map(y => f(x, y)))
//   for x <- e if p(x) yield x                ⇒ e.withFilter(x => p(x)).map(x => x)
//   for x <- e; y = g(x) yield y              ⇒ e.map { x => val y = g(x); (x, y) }.map((x, y) => y)
//   for x <- e do f(x)                        ⇒ e.foreach(x => f(x))
//   for case P(x) <- e yield x                ⇒ e.withFilter { case P(_) => true; case _ => false }.map { case P(x) => x }
//
// See the expansion yourself:  scala-cli compile file.scala -O -Xprint:typer

package advanced.forcomp

// ---------- a type that supports for, built from scratch ----------
// A tiny "Reader"-like log-collecting type: value + log lines
final case class Logged[A](value: A, log: List[String]):
  def map[B](f: A => B): Logged[B] = Logged(f(value), log)
  def flatMap[B](f: A => Logged[B]): Logged[B] =
    val next = f(value)
    Logged(next.value, log ++ next.log)

def logged[A](a: A, msg: String): Logged[A] = Logged(a, List(msg))

// A type with foreach only: usable in `for ... do` but not `yield`
class Countdown(n: Int):
  def foreach(f: Int => Unit): Unit = (n to 1 by -1).foreach(f)

// A type with withFilter, to support guards
final case class Box[A](items: List[A]):
  def map[B](f: A => B): Box[B] = Box(items.map(f))
  def flatMap[B](f: A => Box[B]): Box[B] = Box(items.flatMap(a => f(a).items))
  def withFilter(p: A => Boolean): Box[A] =
    println("  (withFilter called)")
    Box(items.filter(p))

@main def run(): Unit =
  section("A custom monad in a for-comprehension")
  val program = for
    a <- logged(2, "got 2")
    b <- logged(a * 10, s"multiplied by 10")
    c <- logged(b + 1, "added 1")
  yield c
  println(program)

  section("Only foreach → `for ... do` works")
  for i <- Countdown(3) do print(s"$i ")
  println()
  println(scala.compiletime.testing.typeCheckErrors(
    "for i <- advanced.forcomp.Countdown(3) yield i").map(_.message.linesIterator.next()))

  section("Guards need withFilter")
  println(for x <- Box(List(1, 2, 3, 4)) if x % 2 == 0 yield x * 10)

  section("Value definitions in for (=)")
  val r = for
    x <- List(1, 2)
    sq = x * x            // ⚠ desugars to a map producing tuples — slightly costly
    y <- List(sq, -sq)
  yield y
  println(r)

  section("Pattern matching in generators")
  val pairs = List((1, "a"), (2, "b"))
  println(for (n, s) <- pairs yield s * n)        // irrefutable tuple pattern: no filter needed
  val opts = List(Some(1), None, Some(3))
  println(for case Some(v) <- opts yield v)       // refutable: `case` makes the filtering explicit

  section("Mixing monads: not allowed")
  // The first generator determines the type; all flatMaps must agree.
  // for { a <- Option(1); b <- List(1, 2) } yield ...  → Option.flatMap needs an Option
  //   (it happens to work for Option+List via implicit conversion Option→Iterable, but
  //    Future+List or Either+Option do not compile).
  println(scala.compiletime.testing.typeCheckErrors(
    "for a <- (Right(1): Either[String, Int]); b <- Some(2) yield a + b").map(_.message.linesIterator.next()))

  section("Side-effect ordering")
  val res = for
    a <- { println("  first generator evaluated"); List(1, 2) }
    b <- { println(s"  second generator evaluated for a=$a"); List(10) }
  yield a + b
  println(res)          // second generator re-evaluated for EACH a (it's inside the lambda)

  section("Desugared by hand")
  val manual = List(1, 2).flatMap(a => List(10).map(b => a + b))
  println(manual == res)

  section("'Better fors' (SIP-62) notes")
  // SIP-62 (introduced in Scala 3.6 as a preview/experimental feature) simplifies the
  // desugaring: a trailing `yield x` that just returns the last bound value drops the final
  // `map`, aliases don't create intermediate tuples, and a `for` may START with an alias.
  // Whether it is on by default depends on your Scala version. Behaviour is the same for
  // lawful monads; it only matters for types whose `map` has side effects or costs.
  println("see comments")

def section(title: String): Unit = println(s"\n=== $title ===")
