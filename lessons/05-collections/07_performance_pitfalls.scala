//> using scala 3.9.0
// Run: scala-cli run lessons/05-collections/07_performance_pitfalls.scala
//
// Collection performance traps — measured. Each body runs once to warm up the JIT, then once
// timed. Timings are still rough: use JMH (sbt-jmh) for real benchmarks. Look at orders of
// magnitude, not small differences.

package collections.performance

import scala.collection.mutable

def time[A](label: String)(body: => A): A =
  body                                   // warm-up run (JIT compilation, class loading)
  val t0 = System.nanoTime()
  val r = body
  println(f"  $label%-50s ${(System.nanoTime() - t0) / 1e6}%8.2f ms")
  r

@main def run(): Unit =
  val n = 20_000

  section("1. Appending to a List in a loop is O(n²)")
  time("List :+ in a loop") { (1 to n).foldLeft(List.empty[Int])(_ :+ _) }
  time("List prepend + reverse") { (1 to n).foldLeft(List.empty[Int])((acc, x) => x :: acc).reverse }
  time("Vector :+ in a loop") { (1 to n).foldLeft(Vector.empty[Int])(_ :+ _) }
  time("ListBuffer then toList") { val b = mutable.ListBuffer.empty[Int]; (1 to n).foreach(b += _); b.toList }

  section("2. Indexing into a List is O(n)")
  val list = List.range(0, n)
  val vec = Vector.range(0, n)
  time("list(i) for every i") { (0 until n by 10).map(list(_)).sum }
  time("vector(i) for every i") { (0 until n by 10).map(vec(_)).sum }

  section("3. .length / .size on List is O(n)")
  time("list.size inside a loop (1000x)") { (1 to 1000).map(_ => list.size).sum }
  time("list.lengthCompare(5) — O(min(n,5))") { (1 to 1000).count(_ => list.lengthCompare(5) > 0) }
  // Use isEmpty/nonEmpty, or lengthCompare / sizeIs:
  println(s"  list.sizeIs > 5 = ${list.sizeIs > 5}")

  section("4. contains on a Seq is O(n); on a Set O(1)")
  val lookups = (0 until 2000).map(_ * 7)
  time("Seq.contains") { lookups.count(list.contains) }
  val set = list.toSet
  time("Set.contains") { lookups.count(set.contains) }

  section("5. Chained transformations allocate intermediates")
  val big = Vector.range(0, 1_000_000)
  time("map.filter.map (3 vectors)") { big.map(_ + 1).filter(_ % 2 == 0).map(_ * 3).sum }
  time("view.map.filter.map (fused)") { big.view.map(_ + 1).filter(_ % 2 == 0).map(_ * 3).sum }
  time("single foldLeft") { big.foldLeft(0L)((acc, x) => { val y = x + 1; if y % 2 == 0 then acc + y * 3 else acc }) }

  section("6. Boxing: generic collections box primitives")
  val arr = Array.range(0, 1_000_000)
  // ⚠ `sum` is generic (it goes through Numeric[Int]), so even on an Array[Int] each element
  //   is boxed; only a hand-written loop over the primitive array avoids boxing entirely.
  time("Array[Int].sum (generic, boxes per element)") { arr.sum }
  time("Vector[Int].sum (elements stored boxed)") { big.sum }
  time("while loop over Array (no boxing)") { var i = 0; var s = 0L; while i < arr.length do { s += arr(i); i += 1 }; s }

  section("7. String concatenation in a loop")
  time("String += in a loop") { var s = ""; for i <- 1 to 5000 do s += i; s.length }
  time("StringBuilder") { val sb = new StringBuilder(); for i <- 1 to 5000 do sb.append(i); sb.length }
  time("mkString") { (1 to 5000).mkString.length }

  section("8. groupBy + mapValues vs groupMapReduce")
  val words = Vector.fill(200_000)(scala.util.Random.nextInt(1000).toString)
  time("groupBy(identity).map(size)") { words.groupBy(identity).map { case (k, v) => k -> v.size }.size }
  time("groupMapReduce") { words.groupMapReduce(identity)(_ => 1)(_ + _).size }

  section("9. Accidental quadratic: distinct via contains")
  time("manual distinct with List.contains") {
    words.take(5000).foldLeft(List.empty[String])((acc, w) => if acc.contains(w) then acc else w :: acc).size
  }
  time("distinct (uses a HashSet)") { words.take(5000).distinct.size }

  section("10. Recursion depth on non-tail-recursive list processing")
  def badSum(xs: List[Int]): Long = xs match
    case Nil    => 0
    case h :: t => h + badSum(t)
  try println(s"  badSum = ${badSum(List.range(0, 1_000_000))}")
  catch case _: StackOverflowError => println("  badSum: StackOverflowError — use foldLeft / @tailrec")

def section(title: String): Unit = println(s"\n=== $title ===")
