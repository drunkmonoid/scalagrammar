//> using scala 3.9.0
// Run: scala-cli run lessons/05-collections/03_sets_maps.scala
//
// Sets and Maps: hashing, ordering, defaults, and update semantics.

package collections.setsmaps

import scala.collection.immutable.{SortedMap, TreeMap, TreeSet, ListMap, VectorMap, BitSet}
import scala.collection.mutable

@main def run(): Unit =
  section("Set basics")
  val s = Set(3, 1, 2)
  println(s + 4)
  println(s - 1)
  println(s ++ Set(9, 10))
  println(s.contains(2) -> s(2))       // apply = contains
  println(s union Set(5) intersect Set(1, 5))
  println(s diff Set(1))
  println(s.subsetOf(Set(1, 2, 3, 4)))
  println(s.map(_ / 2))                 // ⚠ size can shrink: Set(1, 0)

  section("Ordering of sets")
  println(Set(5, 1, 4, 2, 3, 9).toList) // ⚠ unspecified order (HashSet, >4 elements)
  println(TreeSet(5, 1, 4))             // sorted
  println(TreeSet("b", "a")(using Ordering[String].reverse))
  println(collection.immutable.ListSet(3, 1, 2))   // insertion order (O(n) ops)
  println(BitSet(10, 2, 7))             // dense ints, sorted, compact

  section("Map basics")
  val m = Map("a" -> 1, "b" -> 2)       // `->` builds a Tuple2
  println(m("a"))
  try m("z") catch case e: NoSuchElementException => println(s"m(\"z\") → $e")
  println(m.get("z"))                   // Option
  println(m.getOrElse("z", 0))
  println(m + ("c" -> 3))
  println(m.updated("a", 100))
  println(m - "a")
  println(m.removed("b"))
  println(m ++ Map("a" -> 9))           // right side wins on key collision
  println(m.keys -> m.values)
  println(m.keySet)
  println(m.contains("a"))
  println(m.map { case (k, v) => k.toUpperCase -> v * 10 })
  println(m.view.mapValues(_ * 2).toMap)  // mapValues is lazy → .view + .toMap
  println(m.filter((_, v) => v > 1))
  println(m.collect { case (k, v) if v > 1 => k })
  println(m.updatedWith("a") {
    case Some(v) => Some(v + 1)          // update
    case None    => Some(1)              // insert
  })
  println(m.updatedWith("a")(_ => None)) // remove
  println(m.foldLeft(0) { case (acc, (_, v)) => acc + v })
  println(m.toSeq.sortBy(-_._2))

  section("Default values")
  val counts = Map.empty[Char, Int].withDefaultValue(0)
  println(counts('x'))                   // 0
  println(counts.get('x'))               // ⚠ None — defaults only affect apply()
  val fallback = Map(1 -> "one").withDefault(k => s"<$k>")
  println(fallback(42))
  println((fallback + (2 -> "two"))(99)) // ⚠ default survives +, but NOT all transformations
  println(fallback.map(identity).getClass.getSimpleName)  // Map1 → default lost after map

  section("Word count, three ways")
  val text = "the cat and the hat and the bat"
  val words = text.split(" ").toList
  println(words.groupBy(identity).view.mapValues(_.size).toMap)
  println(words.groupMapReduce(identity)(_ => 1)(_ + _))
  val mc = mutable.Map.empty[String, Int]
  words.foreach(w => mc.updateWith(w)(c => Some(c.getOrElse(0) + 1)))
  println(mc)

  section("Ordered maps")
  println(TreeMap(3 -> "c", 1 -> "a", 2 -> "b"))
  val sm = SortedMap(10 -> "x", 20 -> "y", 30 -> "z")
  println(sm.rangeFrom(15))              // keys >= 15
  println(sm.rangeUntil(20))
  println(sm.minAfter(11))               // Some((20,y))
  println(sm.maxBefore(30))
  println(ListMap("z" -> 1, "a" -> 2))   // insertion order
  println(VectorMap("z" -> 1, "a" -> 2) + ("m" -> 3))   // insertion order, fast
  // ⚠ a plain Map keeps insertion order up to 4 entries (Map1..Map4), then becomes a HashMap:
  println(Map(5 -> 0, 4 -> 0, 3 -> 0, 2 -> 0).keys)
  println(Map(5 -> 0, 4 -> 0, 3 -> 0, 2 -> 0, 1 -> 0).keys)

  section("Keys must have stable equals/hashCode")
  case class Key(id: Int)
  println(Map(Key(1) -> "x")(Key(1)))    // works: case classes have structural hashCode
  class NoEq(val id: Int)
  println(Map(NoEq(1) -> "x").get(NoEq(1)))  // ⚠ None — identity hashCode
  // Arrays as keys: also identity — convert to Seq/List/ArraySeq first.
  println(Map(Array(1).toSeq -> "ok").get(Seq(1)))

  section("Double / NaN / -0.0 keys")
  // Scala collections compare elements with == and hash them with ##, both of which follow
  // IEEE semantics for doubles (unlike Java's Double.equals):
  println(Set(0.0, -0.0).size)               // 1 — 0.0 == -0.0
  println(Set(Double.NaN, Double.NaN).size)  // ⚠ 2 — NaN != NaN, so it can never be found again
  println(Set(Double.NaN).contains(Double.NaN))  // ⚠ false
  // java.util.HashSet uses Double.equals, which says the opposite on both counts.

  section("Mutable maps & sets")
  val mm = mutable.HashMap("a" -> 1)
  mm("b") = 2                             // update
  mm += ("c" -> 3)
  mm -= "a"
  println(mm.getOrElseUpdate("d", 4))
  println(mm.remove("zz"))                // None
  mm.filterInPlace((_, v) => v % 2 == 0)
  println(mm)
  val linked = mutable.LinkedHashMap("z" -> 1, "a" -> 2)   // insertion order
  println(linked)
  val ms = mutable.Set(1, 2)
  println(ms.add(2))                      // false: already present
  println(ms.add(3))
  // ⚠ Mutating a collection while iterating over it is undefined behaviour:
  //   for k <- mm.keys do mm.remove(k)  → may skip entries or throw. Iterate over a copy.

  section("Java interop")
  import scala.jdk.CollectionConverters.*
  val jmap = new java.util.HashMap[String, Int]()
  jmap.put("j", 1)
  val asScala: mutable.Map[String, Int] = jmap.asScala   // a VIEW, not a copy
  asScala("k") = 2
  println(jmap)                            // {j=1, k=2}
  println(Map("x" -> 1).asJava.getClass.getSimpleName)

def section(title: String): Unit = println(s"\n=== $title ===")
