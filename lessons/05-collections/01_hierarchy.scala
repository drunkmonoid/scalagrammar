//> using scala 3.9.0
// Run: scala-cli run lessons/05-collections/01_hierarchy.scala
//
// The shape of scala.collection (2.13+ redesign, used by Scala 3):
//
//                          IterableOnce
//                               │
//                            Iterable
//                 ┌─────────────┼──────────────┐
//                Seq           Set            Map
//          ┌──────┴──────┐      │              │
//      IndexedSeq   LinearSeq  SortedSet    SortedMap
//    (Vector, ArraySeq, (List, LazyList,
//     Range, String*)   Queue)
//
// Three packages mirror this tree:
//   scala.collection            — read-only interfaces (could be mutable underneath!)
//   scala.collection.immutable  — the default (`List`, `Map`, `Set`, `Vector` in scope)
//   scala.collection.mutable    — ArrayBuffer, HashMap, ListBuffer, ...
// Default `Seq` = immutable.Seq in 2.13+/3 (it was collection.Seq in 2.12).

package collections.hierarchy

import scala.collection.mutable

@main def run(): Unit =
  section("Defaults in scope (from the scala package / Predef)")
  val s: Seq[Int] = Seq(1, 2, 3)
  val is: IndexedSeq[Int] = IndexedSeq(1, 2, 3)
  val l = List(1, 2, 3)
  val v = Vector(1, 2, 3)
  val st = Set(1, 2, 3)
  val m = Map("a" -> 1)
  println(s"Seq() → ${s.getClass.getSimpleName}")          // $colon$colon, i.e. `::` — a List cell
  println(s"IndexedSeq() → ${is.getClass.getSimpleName}")  // Vector (Vector1)
  println(s"Set(1,2,3) → ${st.getClass.getSimpleName}")    // Set3 — specialised small sets!
  println(s"Set of 5 → ${Set(1, 2, 3, 4, 5).getClass.getSimpleName}")  // HashSet
  println(s"Map(a->1) → ${m.getClass.getSimpleName}")      // Map1
  println(s"$l $v")

  section("Uniform return type principle")
  // Operations return the SAME collection type when possible:
  println(List(1, 2).map(_ + 1))           // List
  println(Vector(1, 2).map(_ + 1))         // Vector
  println(Set(1, 2).map(_ % 2))            // Set(1, 0)
  println(Map(1 -> "a").map { case (k, v) => (v, k) })   // Map(a -> 1)
  println(Map(1 -> "a").map { case (k, v) => v })        // ⚠ Iterable — not pairs any more
  println("abc".map(_.toUpper))                  // String
  println((1 to 3).map(_ * 2))                   // Range → IndexedSeq (Vector)
  println(collection.immutable.BitSet(1, 2).map(_ + 1))  // BitSet
  println(collection.immutable.BitSet(1, 2).map(_.toString))  // ⚠ SortedSet[String]: falls back

  section("Converting between collections")
  println(List(1, 2, 2).toSet)
  println(Set(3, 1, 2).toList)             // ⚠ order of a HashSet is unspecified
  println(List("a" -> 1).toMap)
  println(Map("a" -> 1).toList)
  println(List(1, 2).toVector)
  println(List(1, 2).toArray.mkString("Array(", ", ", ")"))
  println(List(3, 1).to(collection.immutable.SortedSet))   // .to(Factory) is generic
  println(List(1, 2).to(mutable.ArrayBuffer))
  println(Iterator(1, 2).to(LazyList))
  // ⚠ .toMap requires an evidence that elements are pairs:
  // List(1, 2).toMap  → compile error "Cannot prove that Int <:< (K, V)"

  section("Read-only vs immutable vs mutable")
  val buf = mutable.ArrayBuffer(1, 2, 3)
  val readOnly: collection.Seq[Int] = buf   // collection.Seq is READ-ONLY, not immutable
  buf += 4
  println(readOnly)                         // ⚠ sees the change
  // immutable.Seq would not accept an ArrayBuffer:
  // val im: Seq[Int] = buf   // error: ArrayBuffer is not an immutable.Seq
  println(buf.toSeq)                        // copies into an immutable List/Vector

  section("Varargs give you an immutable.Seq (ArraySeq) — Scala 2.13+/3")
  def f(xs: Int*) = xs.getClass.getSimpleName
  println(f(1, 2))

  section("Equality across collection types")
  println(List(1, 2) == Vector(1, 2))       // true — both Seqs
  println(Set(1, 2) == collection.immutable.TreeSet(2, 1))   // true — both Sets
  // ⚠ Seq vs Set can never be equal, and Scala 3 rejects the comparison at compile time:
  println(scala.compiletime.testing.typeCheckErrors("List(1, 2) == Set(1, 2)").map(_.message))
  println(Array(1, 2) == Array(1, 2))       // ⚠ false — arrays are Java arrays (reference eq)
  println(Array(1, 2).sameElements(Array(1, 2)))  // true

  section("Factories")
  println(List.fill(3)("x"))
  println(List.tabulate(4)(i => i * i))
  println(List.range(0, 10, 3))
  println(List.iterate(1, 5)(_ * 3))
  println(List.unfold(10)(n => if n == 0 then None else Some((n, n / 2))))
  println(Vector.empty[Int])
  println(Seq.concat(List(1), Vector(2)))
  println(Map.from(List("k" -> 1)))
  println(mutable.Map.empty[String, Int].withDefaultValue(0)("missing"))

  section("Strings & arrays are wrapped implicitly")
  // "abc" gets StringOps (extension) and can be viewed as WrappedString (a Seq[Char]):
  val sq: Seq[Char] = "abc"                 // implicit conversion to WrappedString
  println(sq)
  val arr = Array(1, 2, 3)
  val asSeq: collection.Seq[Int] = arr      // wraps as ArraySeq (mutable)
  println(asSeq)

def section(title: String): Unit = println(s"\n=== $title ===")
