//> using scala 3.9.0
// Run: scala-cli run lessons/05-collections/05_views_iterators_lazylist.scala
//
// Laziness in collections: Iterator (single-pass), View (re-evaluated, lazy transforms),
// LazyList (memoised, possibly infinite).

package collections.laziness

@main def run(): Unit =
  section("Strict pipelines create intermediate collections")
  val strict = (1 to 5).map { x => println(s"  map $x"); x * 2 }.filter { x => println(s"  filter $x"); x > 4 }
  println(strict)                      // all maps run first, then all filters

  section("Views fuse operations and run them on demand")
  val view = (1 to 5).view.map { x => println(s"  map $x"); x * 2 }.filter { x => println(s"  filter $x"); x > 4 }
  println("  (nothing has run yet)")
  println(view.headOption)             // runs just enough: map 1, filter 2, map 2, filter 4, map 3, filter 6
  println(view.toList)                 // ⚠ RE-RUNS everything: views don't memoise
  // Get back to a strict collection with .toList / .toVector / .to(Factory)

  section("Iterator: single pass, lazy, mutable cursor")
  val it = Iterator(1, 2, 3, 4).map(_ * 10)
  println(it.next())                   // 10
  println(it.hasNext)
  println(it.toList)                   // List(20, 30, 40) — the rest
  println(it.toList)                   // ⚠ List() — exhausted
  // ⚠ Many methods consume the iterator, even "queries" like size/length:
  val it2 = Iterator(1, 2, 3)
  println(it2.size)                    // 3
  println(it2.hasNext)                 // false!
  // duplicate() gives two independent iterators (buffers the difference)
  val (a, b) = Iterator(1, 2, 3).duplicate
  println(a.toList -> b.toList)
  // buffered: peek with .head
  val bi = Iterator(5, 6).buffered
  println(bi.head -> bi.next())
  // Iterators are ideal for streaming data, e.g. lines of a file:
  val lines = scala.io.Source.fromString("l1\nl2\nl3").getLines()
  println(lines.filter(_.endsWith("2")).toList)

  section("LazyList: lazy, MEMOISED, can be infinite")
  val naturals: LazyList[Int] = LazyList.from(1)
  println(naturals.take(5).toList)
  val evens = naturals.filter(_ % 2 == 0)
  println(evens.take(3).toList)
  // Self-referential definitions (#:: is lazy in both head and tail)
  val fibs: LazyList[BigInt] = BigInt(0) #:: BigInt(1) #:: fibs.zip(fibs.tail).map(_ + _)
  println(fibs(80))
  // Memoisation: elements are computed once
  val noisy = LazyList.from(1).map { x => println(s"  computing $x"); x }
  println(noisy.take(2).toList)
  println(noisy.take(3).toList)        // only "computing 3" this time
  // Sieve of Eratosthenes
  def sieve(s: LazyList[Int]): LazyList[Int] = s.head #:: sieve(s.tail.filter(_ % s.head != 0))
  println(sieve(LazyList.from(2)).take(10).toList)
  // ⚠ Holding the head of a huge LazyList keeps every computed element in memory.
  //   Use `def` (not `val`) for big streams, or use Iterator.
  // ⚠ Infinite + strict operation = hang:  LazyList.from(1).size  never returns
  // ⚠ toString only shows evaluated elements:
  val partial = LazyList(1, 2, 3).map(_ + 1)
  println(partial)                     // LazyList(<not computed>)
  partial.head
  println(partial)                     // LazyList(2, <not computed>)

  section("Iterator / LazyList generators")
  println(Iterator.iterate(1)(_ * 2).take(8).toList)
  println(Iterator.continually("x").take(3).mkString)
  println(Iterator.unfold(1)(n => Option.when(n < 100)((n, n * 3))).toList)
  // ⚠ `continually` takes a by-name argument: a NEW Random(42) is created per element,
  //   so every element is the same "random" number.
  println(LazyList.continually(scala.util.Random(42).nextInt(10)).take(3).toList)
  val rnd = scala.util.Random(42)
  println(LazyList.continually(rnd.nextInt(10)).take(3).toList)
  println(Iterator.range(0, 10, 4).toList)

  section("When to use which")
  // Iterator  — one pass over potentially huge/IO-bound data; minimal memory
  // View      — fuse several transformations of an existing collection, then materialise
  // LazyList  — need re-traversal or self-reference with caching; infinite sequences
  println(
    (1 to 1_000_000).view.map(_ * 2).filter(_ % 3 == 0).take(3).toList
  )

  section("⚠ View pitfalls")
  var counter = 0
  val v = List(1, 2, 3).view.map { x => counter += 1; x }
  v.sum; v.sum
  println(s"map ran $counter times for 3 elements")   // 6
  // Side effects inside views run at unpredictable times — keep views pure.
  // `mapValues` and `filterKeys` on Map return views (MapView) in 2.13+, so
  // the function runs on every lookup:
  var calls = 0
  val mv = Map(1 -> 1).view.mapValues { x => calls += 1; x * 2 }
  mv(1); mv(1)
  println(s"mapValues function ran $calls times")

def section(title: String): Unit = println(s"\n=== $title ===")
