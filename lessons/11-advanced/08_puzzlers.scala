//> using scala 3.9.0
// Run: scala-cli run lessons/11-advanced/08_puzzlers.scala
//
// Puzzlers: code whose behaviour surprises experienced developers. Guess each answer
// before reading the explanation, then run the file.

package advanced.puzzlers

@main def run(): Unit =
  puzzle("1. Set or List?") {
    val xs = List(1, 2, 3)
    xs.toSet.map((x: Int) => x / 2).sum      // ?
  }
  // 2 — {0, 1, 1} collapses to Set(0, 1). map on a Set returns a Set.

  puzzle("2. Default arguments and overriding") {
    class A { def f(x: Int = 1) = x }
    class B extends A { override def f(x: Int = 2) = x }
    val a: A = B()
    a.f()                                     // ?
  }
  // 2 — default arguments are resolved by the RUNTIME class's default getter (f$default$1),
  //     which B overrides. Static type A doesn't matter.

  puzzle("3. The case of the missing apply") {
    val m = Map("a" -> 1)
    m.getOrElse("b", "zero")                  // ?  (what's the static type?)
  }
  // "zero" — and the result type is Matchable/Any (LUB of Int and String), not a compile error.

  puzzle("4. Average") {
    val xs = List(1, 2)
    xs.sum / xs.size                          // ?
  }
  // 1 — Int / Int is integer division. Use xs.sum.toDouble / xs.size for 1.5.

  puzzle("5. Iterator reuse") {
    val it = Iterator(1, 2, 3)
    val doubled = it.map(_ * 2)
    (it.toList, doubled.toList)               // ?
  }
  // (List(1, 2, 3), List()) — `doubled` wraps the SAME iterator, which `it.toList`
  // already exhausted.

  puzzle("6. Option(null) vs Some(null)") {
    (Option(null), Some(null))                // (None, Some(null))
  }

  puzzle("7. Overridden val and init order") {
    trait T { val a = 10; val b = a * 2 }
    class C extends T { override val a = 1 }
    C().b                                     // ?
  }
  // 0 — when T's initializer computes b, C's override of `a` isn't initialised yet (0).

  puzzle("8. Map.mapValues is lazy") {
    var calls = 0
    val m = Map(1 -> 1).view.mapValues { v => calls += 1; v }.toMap
    m(1); m(1)
    calls                                      // 1 — .toMap forced it once; without .toMap: 2+
  }

  puzzle("9. Equality of floating point in collections") {
    List(Double.NaN).contains(Double.NaN)     // false — NaN != NaN
  }

  puzzle("10. String + Int + Int") {
    "1" + 2 + 3                               // "123", while 1 + 2 + "3" is "33"
  }

  puzzle("11. Char arithmetic") {
    ('a' + 'b').toString                      // "195" — Char + Char is Int
  }

  puzzle("12. Tuple in a single-arg call (auto-tupling)") {
    def f(x: Any) = x
    f(1, 2)                                   // (1,2) — auto-tupled (deprecated warning)
  }

  puzzle("13. `val` in pattern with uppercase") {
    val Limit = 10
    5 match
      case Limit => "matched Limit"
      case _     => "no match"                // "no match": Limit is a stable identifier (== 10)
  }

  puzzle("14. List vs Vector equality") {
    List(1, 2) == Vector(1, 2)                // true — Seq equality ignores the implementation
  }

  puzzle("15. Statement after an `if` without else") {
    def g(b: Boolean): Any = if b then "yes"
    g(false)                                  // () — missing else is `else ()`
  }

def puzzle(title: String)(body: => Any): Unit =
  println(f"$title%-45s → ${body}")
