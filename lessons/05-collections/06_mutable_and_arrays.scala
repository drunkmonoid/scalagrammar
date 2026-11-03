//> using scala 3.9.0
// Run: scala-cli run lessons/05-collections/06_mutable_and_arrays.scala
//
// Mutable collections and Arrays (JVM arrays: mutable, fixed-size, reified, invariant,
// reference equality — the odd one out).

package collections.mutablearrays

import scala.collection.mutable
import scala.compiletime.testing.typeCheckErrors
import scala.reflect.ClassTag

@main def run(): Unit =
  section("ArrayBuffer: the mutable workhorse (amortised O(1) append, O(1) index)")
  val buf = mutable.ArrayBuffer(1, 2, 3)
  buf += 4                       // append
  buf ++= Seq(5, 6)
  0 +=: buf                      // prepend (O(n))
  buf.insert(2, 99)
  buf.remove(0)
  buf(0) = 100                   // update
  println(buf)
  buf.mapInPlace(_ * 2).filterInPlace(_ > 10)
  println(buf)
  buf.sortInPlace()
  println(buf)
  buf.clear()
  println(buf.isEmpty)

  section("ListBuffer: O(1) append, then toList for free(ish)")
  val lb = mutable.ListBuffer.empty[String]
  for i <- 1 to 3 do lb += s"item$i"
  val frozen = lb.toList         // ⚠ copy-on-write afterwards: cheap
  lb += "late"
  println(frozen -> lb)

  section("Other mutable collections")
  val stack = mutable.Stack(1, 2)
  stack.push(3)
  println(stack.pop() -> stack)
  val queue = mutable.Queue(1, 2)
  queue.enqueue(3)
  println(queue.dequeue() -> queue)
  val pq = mutable.PriorityQueue(5, 1, 9)             // max-heap by default
  println(Iterator.continually(pq.dequeue()).take(3).toList)
  val minPq = mutable.PriorityQueue(5, 1, 9)(using Ordering[Int].reverse)
  println(minPq.dequeue())
  // ⚠ PriorityQueue.toString / iteration order is NOT sorted — only dequeue is.
  println(mutable.PriorityQueue(3, 1, 2).toList)
  val deque = mutable.ArrayDeque(1, 2, 3)
  deque.prepend(0); deque.append(4)
  println(deque.removeHead() -> deque.removeLast())
  val bits = mutable.BitSet(1, 5)
  bits += 64
  println(bits)
  val sb = new mutable.StringBuilder()
  sb.append("ab").append(1).append('c')
  println(sb.result())

  section("Builders: efficient construction of immutable collections")
  val vb = Vector.newBuilder[Int]
  for i <- 1 to 5 do vb += i * i
  println(vb.result())
  val mb = Map.newBuilder[String, Int]
  mb += "a" -> 1
  println(mb.result())

  section("Arrays: creation")
  val a = Array(1, 2, 3)
  val zeros = Array.ofDim[Int](3)          // Array(0, 0, 0)
  val nulls = new Array[String](2)         // Array(null, null)
  val grid = Array.ofDim[Int](2, 3)        // 2×3 matrix
  val filled = Array.fill(3)("x")
  val tab = Array.tabulate(4)(i => i * i)
  println(a.mkString(","))
  println(zeros.mkString(","))
  println(nulls.mkString(","))
  println(grid.map(_.mkString(" ")).mkString(" | "))
  println(filled.toList)
  println(tab.toSeq)                       // ArraySeq: nice printing
  println(a)                               // ⚠ [I@1b6d3586 — Java's toString
  println(java.util.Arrays.toString(a))

  section("Arrays: mutation, equality, hashing")
  a(0) = 10
  println(a.toList)
  val b = Array(10, 2, 3)
  println(a == b)                          // ⚠ false (reference equality)
  println(a.sameElements(b))               // true
  println(java.util.Arrays.equals(a, b))   // true
  println(a.toSeq == b.toSeq)              // true
  println(Array(Array(1)).sameElements(Array(Array(1))))   // ⚠ false: nested arrays compared by ref
  println(java.util.Arrays.deepEquals(Array[AnyRef](Array(1)), Array[AnyRef](Array(1))))

  section("Arrays are invariant (unlike Java)")
  val strs: Array[String] = Array("a")
  doesNotCompile("val objs: Array[Any] = Array[String](\"a\")")
  // Java's covariant arrays allow ArrayStoreException at runtime; Scala prevents it statically.
  val objs: Array[Any] = strs.map(identity)  // copy into a new Array[Any]
  println(objs.toList)

  section("Arrays are reified: generic creation needs a ClassTag")
  def makeArray[T: ClassTag](n: Int, x: T): Array[T] = Array.fill(n)(x)
  println(makeArray(2, "s").getClass.getSimpleName)   // String[]
  println(makeArray(2, 1).getClass.getSimpleName)     // int[] — a primitive array!
  doesNotCompile("def bad[T](n: Int): Array[T] = new Array[T](n)")

  section("Array operations come from ArrayOps (extension) — return Arrays")
  val doubled: Array[Int] = a.map(_ * 2)
  println(doubled.mkString(","))
  println(a.filter(_ > 2).mkString(","))
  println(a.sorted.reverse.mkString(","))
  println(a.sum -> a.max)
  // In-place utilities
  val arr = Array(5, 3, 1)
  scala.util.Sorting.quickSort(arr)
  println(arr.mkString(","))
  java.util.Arrays.fill(arr, 7)
  println(arr.mkString(","))
  val copy = arr.clone()
  copy(0) = 0
  println(arr(0) -> copy(0))               // independent
  Array.copy(Array(1, 2, 3), 0, arr, 1, 2) // System.arraycopy
  println(arr.mkString(","))

  section("Arrays and varargs / Java interop")
  def count(xs: Int*): Int = xs.size
  println(count(a*))                       // splice an array into varargs
  println(String.join("-", Array("x", "y")*)) // Java varargs
  // main(args: Array[String]) is the canonical array in Scala programs.

  section("⚠ Pitfall: Array as a default parameter or val in an object is SHARED mutable state")
  println(Shared.defaults.mkString(","))
  Shared.defaults(0) = -1
  println(Shared.defaults.mkString(","))   // changed for everyone — prefer IArray / immutable

  section("IArray: immutable arrays (Scala 3)")
  val ia: IArray[Int] = IArray(1, 2, 3)   // opaque type over Array, no copying
  println(ia(1))
  doesNotCompile("val ia = IArray(1, 2); ia(0) = 5")
  println(ia.map(_ + 1).toList)

object Shared:
  val defaults = Array(1, 2, 3)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
