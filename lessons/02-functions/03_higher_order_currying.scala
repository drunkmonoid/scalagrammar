//> using scala 3.9.0
// Run: scala-cli run lessons/02-functions/03_higher_order_currying.scala
//
// Higher-order functions (HOFs), currying, partial application, and common FP combinators.

package functions.hof

@main def run(): Unit =
  section("Functions that take functions")
  def applyTwice(f: Int => Int, x: Int): Int = f(f(x))
  println(applyTwice(_ + 3, 10))      // 16
  def mapAll[A, B](xs: List[A], f: A => B): List[B] =
    xs match
      case Nil    => Nil
      case h :: t => f(h) :: mapAll(t, f)
  println(mapAll(List(1, 2, 3), _.toString * 2))

  section("Functions that return functions")
  def adder(n: Int): Int => Int = x => x + n
  val add5 = adder(5)
  println(add5(10))
  def compose[A, B, C](g: B => C, f: A => B): A => C = a => g(f(a))
  println(compose((s: String) => s.length, (i: Int) => "x" * i)(4))

  section("Currying")
  val addC: Int => Int => Int = a => b => a + b
  println(addC(1)(2))
  val add3 = (a: Int, b: Int, c: Int) => a + b + c
  val add3C = add3.curried            // Int => Int => Int => Int
  println(add3C(1)(2)(3))
  val back = Function.uncurried(add3C)
  println(back(1, 2, 3))

  section("Partial application")
  def volume(l: Int, w: Int, h: Int) = l * w * h
  val withL2 = volume(2, _, _)        // (Int, Int) => Int
  println(withL2(3, 4))
  val onlyH = volume(1, 1, _: Int)    // type ascription sometimes needed
  println(onlyH(9))

  section("The standard combinators")
  val xs = List(1, 2, 3, 4, 5, 6)
  println(xs.map(_ * 2))
  println(xs.filter(_ % 2 == 0))
  println(xs.filterNot(_ % 2 == 0))
  println(xs.flatMap(x => List(x, -x)))
  println(xs.foldLeft(0)(_ + _))
  println(xs.foldRight(List.empty[Int])(_ :: _))
  println(xs.reduce(_ max _))
  println(xs.scanLeft(0)(_ + _))       // running totals
  println(xs.collect { case x if x > 3 => x * 100 })
  println(xs.partition(_ < 4))
  println(xs.groupBy(_ % 3))
  println(xs.groupMapReduce(_ % 2 == 0)(identity)(_ + _))
  println(xs.takeWhile(_ < 3))
  println(xs.dropWhile(_ < 3))
  println(xs.span(_ < 3))
  println(xs.exists(_ > 5))
  println(xs.forall(_ > 0))
  println(xs.find(_ > 3))
  println(xs.count(_ % 2 == 1))
  println(xs.sortBy(-_))
  println(xs.maxBy(x => math.abs(x - 3)))

  section("foldLeft vs foldRight — order and stack")
  println(List("a", "b", "c").foldLeft("")((acc, s) => s"($acc+$s)"))   // (((+a)+b)+c)
  println(List("a", "b", "c").foldRight("")((s, acc) => s"($s+$acc)"))  // (a+(b+(c+)))
  // List.foldRight is implemented by reversing + foldLeft, so it is stack-safe for List,
  // but a naive recursive foldRight is not.

  section("Writing a HOF with a by-name parameter: a tiny `retry`")
  def retry[A](times: Int)(action: => A): A =
    try action
    catch case e: Exception if times > 1 =>
      println(s"  failed (${e.getMessage}), retrying")
      retry(times - 1)(action)
  var calls = 0
  val r = retry(3) {
    calls += 1
    if calls < 3 then throw RuntimeException(s"attempt $calls") else "ok"
  }
  println(s"$r after $calls calls")

  section("Memoization with a HOF")
  def memo[A, B](f: A => B): A => B =
    val cache = collection.mutable.HashMap.empty[A, B]
    a => cache.getOrElseUpdate(a, f(a))
  val slowSquare = (x: Int) => { println(s"  computing $x²"); x * x }
  val fastSquare = memo(slowSquare)
  println(fastSquare(9)); println(fastSquare(9))
  // ⚠ recursive memoization requires the recursive call to go through the memoized fn:
  lazy val fib: BigInt => BigInt = memo { n => if n < 2 then n else fib(n - 1) + fib(n - 2) }
  println(fib(90))

  section("Function as strategy / dependency")
  case class Discount(run: Double => Double)
  val tenPercent = Discount(_ * 0.9)
  val flat5 = Discount(p => (p - 5).max(0))
  println(List(tenPercent, flat5).map(_.run(100)))

def section(title: String): Unit = println(s"\n=== $title ===")
