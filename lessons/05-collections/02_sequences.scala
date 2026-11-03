//> using scala 3.9.0
// Run: scala-cli run lessons/05-collections/02_sequences.scala
//
// List, Vector, ArraySeq, Range, Queue, LazyList (see 05) — and when to use which.
//
//                 head  tail  apply(i)  prepend  append  update(i)
//   List          O(1)  O(1)  O(n)      O(1)     O(n)    O(n)
//   Vector        eC    eC    eC        eC       eC      eC        (eC = effectively constant)
//   ArraySeq      O(1)  O(n)  O(1)      O(n)     O(n)    O(n)
//   Queue         aC    aC    O(n)      O(1)     O(1)    O(n)      (aC = amortised constant)
//   Range         O(1)  O(1)  O(1)      –        –       –         (no elements stored)

package collections.sequences

import scala.collection.immutable.{ArraySeq, Queue}

@main def run(): Unit =
  section("List: singly-linked, immutable, structural sharing")
  val xs = List(1, 2, 3)
  val ys = 0 :: xs                 // O(1): shares xs
  println(ys)
  println(ys.tail eq xs)           // true — no copy
  println(xs.head -> xs.tail)
  println(xs.headOption -> Nil.headOption)
  try Nil.head catch case e: NoSuchElementException => println(s"Nil.head → $e")
  println(xs :+ 4)                 // O(n) append — copies the whole list
  println(xs ++ List(4, 5))
  println(xs ::: List(4, 5))       // list-specific prepend-all
  println(xs.reverse)
  println(xs.last)                 // O(n)
  println(xs.init)
  println(List(3, 1, 2).sorted)
  // Building a List efficiently: prepend then reverse, or ListBuffer
  val built = (1 to 5).foldLeft(List.empty[Int])((acc, i) => i :: acc).reverse
  println(built)

  section("Vector: wide tree (32-way), balanced for everything")
  val v = Vector.tabulate(10)(identity)
  println(v(7))
  println(v.updated(7, 70))        // new vector; v unchanged
  println(v :+ 10)
  println(-1 +: v)
  println(v.patch(2, Seq(99, 98), 3))   // replace 3 elements from index 2

  section("ArraySeq: immutable wrapper around an Array (fast index, cheap creation)")
  val as = ArraySeq(1, 2, 3)
  println(as(1))
  val unsafe = ArraySeq.unsafeWrapArray(Array(1, 2, 3))   // no copy — don't mutate the array!
  println(unsafe)

  section("Range: lazy arithmetic sequence")
  val r = 1 to 10 by 3
  println(r)                       // a Range: its toString shows the bounds, not elements
  println(r.toList)
  println((1 until 10).size)
  println((10 to 1 by -1).take(3).toList)
  println((1 to 0).isEmpty)        // ⚠ empty: step is +1 by default
  println(('a' to 'e').mkString)   // NumericRange[Char]
  println((1L to 5L).sum)
  // ⚠ `0.0 to 1.0 by 0.25` no longer compiles (Double ranges were removed in 2.13 because of
  // rounding surprises). Use BigDecimal ranges or map over an Int range:
  println((BigDecimal(0) to BigDecimal(1) by BigDecimal("0.25")).toList)
  println((0 to 4).map(_ * 0.25))
  println((1 to Int.MaxValue).size) // no allocation: 2147483647
  // ⚠ ranges with > Int.MaxValue elements throw on size:
  try (Int.MinValue to Int.MaxValue).size
  catch case e: IllegalArgumentException => println(s"huge range: ${e.getMessage.take(50)}...")

  section("Queue: FIFO via two lists")
  val q = Queue(1, 2).enqueue(3)
  val (front, rest) = q.dequeue
  println(s"front=$front rest=$rest")
  println(Queue.empty[Int].dequeueOption)

  section("Seq operations specific to sequences")
  val s = Seq(5, 3, 8, 3, 1)
  println(s.indexOf(3))            // 1
  println(s.lastIndexOf(3))        // 3
  println(s.indexWhere(_ > 4, 1))  // 2 — search from index 1
  println(s.contains(8))
  println(s.distinct)
  println(s.sorted)
  println(s.sortWith(_ > _))
  println(s.sortBy(x => (x % 2, x)))
  println(s.reverse)
  println(s.updated(0, 50))
  println(s.padTo(7, 0))
  println(s.startsWith(Seq(5, 3)))
  println(s.endsWith(Seq(1)))
  println(s.containsSlice(Seq(8, 3)))
  println(s.diff(Seq(3)))          // removes ONE occurrence of 3 (multiset semantics)
  println(s.intersect(Seq(3, 3, 9)))
  println(s.union(Seq(9)))         // same as ++ (keeps duplicates)
  println(s.combinations(2).take(3).toList)
  println(Seq(1, 2, 3).permutations.toList.size)
  println(s.lift(10))              // Option — safe apply
  println(s.isDefinedAt(4))
  println(s.segmentLength(_ > 2))  // length of prefix satisfying p = 4
  println(s.search(8))             // binary search — ⚠ requires sorted input; result is meaningless here
  println(s.sorted.search(8))      // Found(4)

  section("Pattern matching on sequences")
  def describe(seq: Seq[Int]): String = seq match
    case Seq()               => "empty"
    case Seq(x)              => s"single $x"
    case head +: tail        => s"head $head, ${tail.size} more"
  println(describe(Vector(1, 2, 3)))
  println(describe(List(1)))

  section("Choosing a sequence")
  // Default to List for recursive head/tail processing and small collections,
  // Vector for indexed access, appends, or big immutable sequences,
  // ArraySeq for primitive-heavy read-only data, ArrayBuffer for local mutation.
  println("see comments")

def section(title: String): Unit = println(s"\n=== $title ===")
