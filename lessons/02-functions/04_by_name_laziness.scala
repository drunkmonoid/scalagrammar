//> using scala 3.9.0
// Run: scala-cli run lessons/02-functions/04_by_name_laziness.scala
//
// Evaluation strategies: strict (by-value), by-name (=> A), lazy val, and how to
// build your own control structures with them.

package functions.byname

@main def run(): Unit =
  section("By-value vs by-name")
  def byValue(x: Int): Unit = { println("  byValue body"); println(s"  x=$x x=$x") }
  def byName(x: => Int): Unit = { println("  byName body"); println(s"  x=$x x=$x") }
  def noisy: Int = { println("  [evaluating arg]"); 42 }
  byValue(noisy)   // arg evaluated ONCE, BEFORE the body
  byName(noisy)    // ⚠ evaluated at EACH use, AFTER entering the body (twice here)
  def unused(x: => Int): String = "never looked at x"
  println(unused(sys.error("boom")))   // never evaluated, so no exception

  section("Cache a by-name with a lazy val")
  def onceAtMost(x: => Int): Unit =
    lazy val cached = x
    println(s"  ${cached + cached}")
  onceAtMost(noisy)

  section("Building control structures")
  def unless(cond: Boolean)(body: => Unit): Unit = if !cond then body
  unless(1 > 2) { println("  1 is not > 2") }

  def myWhile(cond: => Boolean)(body: => Unit): Unit =
    if cond then
      body
      myWhile(cond)(body)
  var i = 0
  myWhile(i < 3) { print(s"$i "); i += 1 }
  println()

  def time[A](label: String)(block: => A): A =
    val t0 = System.nanoTime
    val r = block
    println(f"  $label took ${(System.nanoTime - t0) / 1e6}%.2f ms")
    r
  val total = time("sum") { (1 to 1_000_000).map(_.toLong).sum }
  println(total)

  // using-style resource management (the stdlib has scala.util.Using; see lesson 08)
  def withResource[R <: AutoCloseable, A](open: => R)(use: R => A): A =
    val r = open
    try use(r) finally r.close()
  val out = withResource(java.io.StringReader("abc")) { r => r.read().toChar }
  println(out)

  section("By-name parameters are function values in disguise")
  // `=> A` compiles to a Function0; it cannot be stored as a plain value without forcing it.
  def capture(x: => Int): () => Int = () => x
  val thunk = capture(noisy)
  println("  captured, nothing evaluated yet")
  println(thunk())
  // ⚠ A class parameter that is by-name cannot be a `val`/`var` field:
  //   class C(val x: => Int)   // error: `val` parameters may not be call-by-name

  section("Short-circuiting is by-name")
  // `&&` and `||` are compiled specially, but you can write your own:
  extension (a: Boolean)
    infix def and(b: => Boolean): Boolean = if a then b else false
  println(false and { println("  never"); true })

  section("lazy val: initialization-on-demand")
  object Config:
    println("  Config object initialised")
    lazy val heavy = { println("  heavy computed"); "data" }
    val light = { println("  light computed"); 1 }
  println("before touching Config")
  println(Config.light)    // initialises the object (and `light`), not `heavy`
  println(Config.heavy)
  println(Config.heavy)

  section("lazy val cycles deadlock or overflow")
  // object A { lazy val a: Int = B.b }; object B { lazy val b: Int = A.a }
  // → StackOverflowError on one thread, or DEADLOCK across threads (pre-3.3 encoding).
  // Scala 3.3+ uses a new lazy val scheme that avoids the deadlock on independent fields,
  // but genuine cycles still never terminate.
  println("(not run — see comment)")

  section("Strict vs lazy collections")
  val strict = List(1, 2, 3).map { x => println(s"  strict map $x"); x * 2 }
  println(strict.head)
  val lazyView = List(1, 2, 3).view.map { x => println(s"  view map $x"); x * 2 }
  println(lazyView.head)   // only evaluates the first element
  val ll = LazyList.from(1).map(_ * 2)    // infinite, evaluated on demand
  println(ll.take(5).toList)

def section(title: String): Unit = println(s"\n=== $title ===")
