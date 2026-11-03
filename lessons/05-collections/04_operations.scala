//> using scala 3.9.0
// Run: scala-cli run lessons/05-collections/04_operations.scala
//
// The operations catalogue, grouped by purpose. Each line prints its result.

package collections.operations

@main def run(): Unit =
  val xs = List(4, 8, 15, 16, 23, 42)
  val words = List("apple", "banana", "cherry", "avocado", "blueberry")

  section("Transform")
  show("map", xs.map(_ / 2))
  show("flatMap", xs.take(2).flatMap(x => List(x, x)))
  show("flatten", List(List(1), Nil, List(2, 3)).flatten)
  show("flatten Options", List(Some(1), None, Some(3)).flatten)
  show("collect", xs.collect { case x if x % 2 == 1 => x * 10 })
  show("zipWithIndex", words.take(2).zipWithIndex)
  show("zip", xs.zip(words))                 // truncates to the shorter
  show("zipAll", List(1, 2, 3).zipAll(List("a"), 0, "?"))
  show("lazyZip", List(1, 2).lazyZip(List(10, 20)).lazyZip(List(100, 200)).map(_ + _ + _))
  show("unzip", List(1 -> 'a', 2 -> 'b').unzip)
  show("unzip3", List((1, 'a', true)).unzip3)
  show("transpose", List(List(1, 2, 3), List(4, 5, 6)).transpose)
  show("scanLeft", xs.scanLeft(0)(_ + _))
  show("scanRight", List(1, 2, 3).scanRight(0)(_ + _))

  section("Filter / select")
  show("filter", xs.filter(_ > 10))
  show("filterNot", xs.filterNot(_ > 10))
  show("withFilter (lazy)", xs.withFilter(_ > 10).map(identity))
  show("take / drop", (xs.take(2), xs.drop(4)))
  show("takeRight / dropRight", (xs.takeRight(2), xs.dropRight(4)))
  show("takeWhile / dropWhile", (xs.takeWhile(_ < 16), xs.dropWhile(_ < 16)))
  show("slice", xs.slice(1, 3))
  show("distinct", List(1, 1, 2, 1).distinct)
  show("distinctBy", words.distinctBy(_.head))
  show("head / last", (xs.head, xs.last))
  show("find", xs.find(_ > 15))
  show("headOption on empty", List.empty[Int].headOption)

  section("Split")
  show("partition", xs.partition(_ % 2 == 0))
  show("partitionMap", xs.partitionMap(x => if x > 10 then Right(x) else Left(x.toString)))
  show("span", xs.span(_ < 16))
  show("splitAt", xs.splitAt(2))
  show("groupBy", words.groupBy(_.head))
  show("groupMap", words.groupMap(_.head)(_.length))
  show("groupMapReduce", words.groupMapReduce(_.head)(_ => 1)(_ + _))
  show("grouped", xs.grouped(4).toList)
  show("sliding", xs.sliding(3).toList)
  show("sliding step", xs.sliding(2, 2).toList)

  section("Aggregate")
  show("sum / product", (xs.sum, List(1, 2, 3, 4).product))
  show("min / max", (xs.min, xs.max))
  show("minBy / maxBy", (words.minBy(_.length), words.maxBy(_.length)))
  show("minOption on empty", List.empty[Int].minOption)
  show("count", xs.count(_ > 10))
  show("size / length", (xs.size, xs.length))
  show("foldLeft", xs.foldLeft("")((acc, x) => acc + x))
  show("foldRight", xs.foldRight("")((x, acc) => acc + x))
  show("fold (assoc op)", xs.fold(0)(_ + _))
  show("reduce", xs.reduce(_ - _))           // ((((4-8)-15)-16)-23)-42
  show("reduceOption on empty", List.empty[Int].reduceOption(_ + _))
  show("aggregate-like: mkString", xs.mkString("<", "|", ">"))
  show("corresponds", List(1, 2).corresponds(List("a", "bb"))((n, s) => s.length == n))

  section("Test")
  show("exists / forall", (xs.exists(_ > 40), xs.forall(_ > 0)))
  show("forall on empty", List.empty[Int].forall(_ > 1000))   // ⚠ true (vacuous truth)
  show("exists on empty", List.empty[Int].exists(_ => true))  // false
  show("contains", xs.contains(15))
  show("isEmpty / nonEmpty", (xs.isEmpty, xs.nonEmpty))
  show("sameElements", xs.sameElements(xs.toVector))

  section("Sort")
  show("sorted", words.sorted)
  show("sortBy", words.sortBy(_.length))
  show("sortBy tuple (length, then alpha desc)", words.sortBy(w => (w.length, w))(using Ordering.Tuple2(using Ordering.Int, Ordering.String.reverse)))
  show("sortWith", words.sortWith(_.length > _.length))
  show("reverse", xs.reverse)
  // ⚠ Sorting is STABLE (equal elements keep their relative order)
  show("stable sortBy", List("bb", "a", "cc", "b").sortBy(_.length))

  section("Combine")
  show("++ / concat", List(1) ++ Vector(2) ++ Set(3))
  show("prepended / appended", (0 +: List(1), List(1) :+ 2))
  show("prependedAll", List(0, 0) ++: List(1))
  show("intersect / diff", (List(1, 2, 3) intersect List(2, 3, 4), List(1, 2, 3) diff List(2)))
  show("padTo", List(1).padTo(3, 0))
  show("patch", List(1, 2, 3, 4).patch(1, List(9), 2))
  show("updated", List(1, 2, 3).updated(1, 20))
  show("tails", List(1, 2, 3).tails.toList)
  show("inits", List(1, 2, 3).inits.toList)

  section("Side effects")
  xs.take(2).foreach(x => print(s"$x "))
  println()
  import scala.util.chaining.*
  val r = xs.map(_ + 1).tap(l => println(s"  tap sees $l")).sum
  println(s"  sum=$r")

  section("⚠ Gotchas")
  // 1. `map` on a Set dedups
  show("Set map", Set(1, 2, 3).map(_ => 0))
  // 2. `sum` on a Map's values vs map — values could be Iterable (duplicates OK) but keySet dedups
  val prices = Map("a" -> 1, "b" -> 1)
  show("values.sum", prices.values.sum)                 // 2
  show("map(_._2).sum (Map → Iterable)", prices.map(_._2).sum)  // 2 (Iterable)
  show("toSet first, then sum", prices.values.toSet.sum) // ⚠ 1
  // 3. Converting a Map with a "map" that produces pairs keeps keys unique:
  show("pairs collapse", Map(1 -> "a", 2 -> "b").map { case (_, v) => ("same", v) })
  // 4. `reduce` on empty throws
  try List.empty[Int].reduce(_ + _)
  catch case e: UnsupportedOperationException => println(s"  reduce on empty → $e")
  // 5. `max` on empty throws
  try List.empty[Int].max
  catch case e: UnsupportedOperationException => println(s"  max on empty → $e")
  // 6. Iterators are single-use
  val it = Iterator(1, 2, 3)
  show("first sum", it.sum)
  show("second sum (exhausted!)", it.sum)

def show(label: String, value: Any): Unit = println(f"  $label%-40s $value")
def section(title: String): Unit = println(s"\n=== $title ===")
