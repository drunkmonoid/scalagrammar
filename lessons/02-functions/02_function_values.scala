//> using scala 3.9.0
// Run: scala-cli run lessons/02-functions/02_function_values.scala
//
// Functions are values: instances of Function0..Function22 (and beyond, via FunctionXXL).

package functions.values

import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("Function literals (lambdas)")
  val inc: Int => Int = x => x + 1
  val add = (a: Int, b: Int) => a + b
  val noArgs = () => "called"
  val typed: (String, Int) => String = (s, n) => s * n
  println(inc(1))
  println(add(2, 3))
  println(noArgs())
  println(typed("ab", 3))
  // A function is an object with an apply method:
  println(inc.apply(1))
  println(inc.isInstanceOf[Function1[?, ?]])

  section("Placeholder syntax `_`")
  val double: Int => Int = _ * 2
  val sum2: (Int, Int) => Int = _ + _       // each _ is a NEW parameter, in order
  println(double(4))
  println(sum2(1, 2))
  println(List(1, 2, 3).map(_ + 1))
  // ⚠ the placeholder expands to the SMALLEST enclosing expression that is not just `_`:
  println(List(1, 2, 3).map(_ * 2 + 1))     // x => x * 2 + 1
  // ⚠ ... but nested calls stop the expansion:
  // `math.abs(_)` is x => math.abs(x), so `math.abs(_) + 1` means (x => math.abs(x)) + 1:
  doesNotCompile("List(1, 2).map(math.abs(_) + 1)")
  // ⚠ you cannot use one parameter twice with _ :
  doesNotCompile("val sq: Int => Int = _ * _")   // that's a 2-arg function
  // ⚠ `_` inside a nested function literal belongs to THAT literal
  println(List(List(1, 2), List(3)).map(_.map(_ * 10)))

  section("Eta-expansion: methods → functions")
  def twice(x: Int): Int = x * 2
  val f = twice                  // automatic in Scala 3 (Scala 2 needed `twice _`)
  println(List(1, 2).map(twice)) // method passed where a function is expected
  def curried(a: Int)(b: Int) = a + b
  val c = curried                // Int => Int => Int
  println(c(1)(2))
  val partial = curried(10)      // Int => Int
  println(partial(5))

  section("Function types")
  // A => B           Function1[A, B]
  // (A, B) => C      Function2[A, B, C]
  // () => A          Function0[A]
  // A => B => C      A => (B => C)    (right-associative)
  // ((A, B)) => C    Function1[(A, B), C]  — takes a tuple!
  val fromTuple: ((Int, Int)) => Int = { case (a, b) => a * b }
  println(fromTuple((3, 4)))
  // Parameter untupling (Scala 3): a 2-arg lambda is accepted where a tuple fn is expected
  println(List((1, 2), (3, 4)).map((a, b) => a + b))   // no `case` needed in Scala 3
  // ... and .tupled / Function.untupled convert explicitly
  println(add.tupled((5, 6)))
  // Functions with many params (> 22) work in Scala 3 (FunctionXXL)
  val many = (a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int,
              a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int,
              a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int, a22: Int,
              a23: Int) => a1 + a23
  println(many(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2))

  section("Variance of functions: contravariant in input, covariant in output")
  // Function1[-A, +B] — a function accepting Any can stand in for one accepting String
  val anyToString: Any => String = _.toString
  val strToAny: String => Any = anyToString   // OK: wider input, narrower output
  println(strToAny("works"))
  doesNotCompile("val bad: Any => String = (s: String) => s")

  section("Composition")
  val plus1 = (x: Int) => x + 1
  val times2 = (x: Int) => x * 2
  println((plus1 andThen times2)(5))   // times2(plus1(5)) = 12
  println((plus1 compose times2)(5))   // plus1(times2(5)) = 11
  val pipeline = List[Int => Int](plus1, times2, plus1).reduce(_ andThen _)
  println(pipeline(1))                 // ((1+1)*2)+1 = 5
  println(Function.chain(List(plus1, times2))(1))
  import scala.util.chaining.*
  println(5.pipe(plus1).pipe(times2))  // left-to-right application
  println(List(3, 1, 2).tap(xs => println(s"  debug: $xs")).sorted)

  section("Closures capture variables, not values")
  var factor = 2
  val scale = (x: Int) => x * factor
  println(scale(10))  // 20
  factor = 3
  println(scale(10))  // ⚠ 30 — the closure sees the updated var
  // Classic trap: building closures in a loop with a shared var
  val fs = scala.collection.mutable.ListBuffer.empty[() => Int]
  var i = 0
  while i < 3 do
    fs += (() => i)   // captures the variable i (boxed in an IntRef)
    i += 1
  println(fs.map(_()))  // ⚠ ListBuffer(3, 3, 3)
  // A for loop creates a fresh val each iteration:
  val gs = for j <- 0 until 3 yield () => j
  println(gs.map(_()))  // Vector(0, 1, 2)

  section("SAM conversion: lambdas for single-abstract-method types")
  val r: Runnable = () => println("  Runnable from lambda")
  r.run()
  trait Validator { def validate(s: String): Boolean }
  val nonEmpty: Validator = _.nonEmpty
  println(nonEmpty.validate(""))
  val cmp: java.util.Comparator[String] = (a, b) => a.length - b.length
  println(java.util.Arrays.asList("ccc", "a", "bb").stream().sorted(cmp).toList)

  section("Polymorphic function types (Scala 3)")
  // A function VALUE that is generic: [A] => List[A] => Option[A]
  val headOpt: [A] => List[A] => Option[A] = [A] => (xs: List[A]) => xs.headOption
  println(headOpt(List(1, 2)))
  println(headOpt(List("a")))
  // Can't do this with plain Function1 — it would fix A at definition time.

  section("Dependent function types (Scala 3)")
  trait Entry { type Key; def key: Key }
  val extract: (e: Entry) => e.Key = e => e.key
  val e1: Entry { type Key = Int } = new Entry { type Key = Int; def key = 42 }
  val k: Int = extract(e1)          // the result type depends on the argument value
  println(k)

  section("Function equality is reference equality")
  val h1 = (x: Int) => x
  val h2 = (x: Int) => x
  println(h1 == h2)  // false
  println(h1 == h1)  // true

  section("Identity and constant functions")
  println(List(1, 2, 3).map(identity))
  println(List(1, 2, 3).map(Function.const(0)))

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
