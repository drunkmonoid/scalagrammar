//> using scala 3.9.0
// Run: scala-cli run lessons/04-pattern-matching/01_patterns.scala
//
// A catalogue of EVERY pattern form, with the rules that trip people up.

package patterns.catalogue

import scala.compiletime.testing.typeCheckErrors

case class Person(name: String, age: Int)
case class Pair[A, B](a: A, b: B)

@main def run(): Unit =
  section("1. Wildcard pattern  _")
  def anything(x: Any) = x match { case _ => "matched anything" }
  println(anything(null))

  section("2. Literal patterns")
  def lit(x: Any) = x match
    case 0        => "zero"
    case 'c'      => "char c"
    case "hello"  => "string hello"
    case true     => "true"
    case 2.5      => "double 2.5"
    case null     => "null literal"
    case _        => "other"
  List(0, 'c', "hello", true, 2.5, null, 99).foreach(v => println(s"  $v → ${lit(v)}"))
  // ⚠ literal patterns use ==, so numeric cooperation applies:
  println(s"  (0L: Any) → ${lit(0L)}")   // "zero" — 0L == 0
  // ⚠ ...which is why 99 above printed "char c": 99 == 'c' (Char 'c' has code 99).

  section("3. Variable patterns (lowercase) vs stable identifiers (Uppercase / `backticks`)")
  val expected = 42
  val Expected = 42
  def check(x: Int) = x match
    case Expected => "equals the constant Expected"
    case other    => s"bound a NEW variable other=$other"
  println(check(42)); println(check(7))
  def check2(x: Int) = x match
    case `expected` => "backticks: compared to the existing val `expected`"
    case _          => "different"
  println(check2(42))
  // ⚠ Classic bug: a lowercase name SHADOWS instead of comparing — and catches everything:
  def buggy(x: Int) = x match
    case expected => s"always matches (expected is a new var = $expected)"
    case _        => "unreachable — the compiler warns about this case"
  println(buggy(1))
  // Qualified names are also stable identifiers:
  def q(x: Double) = x match
    case math.Pi => "pi!"
    case _       => "not pi"
  println(q(math.Pi))

  section("4. Typed patterns  x: T")
  def typed(x: Any) = x match
    case i: Int             => s"Int $i"
    case s: String          => s"String of ${s.length}"
    case _: Double | _: Float => "some floating"   // alternative of typed patterns (no binding)
    case xs: List[?]        => s"a list (element type unknown at runtime)"
    case f: (Int => Int) @unchecked => s"function? ${f(1)}"
    case _                  => "?"
  List(1, "abc", 1.0, 1f, List(1), (x: Int) => x + 1).foreach(v => println(s"  ${typed(v)}"))
  // ⚠ null never matches a typed pattern:
  println(typed(null))      // "?" — `case s: String` doesn't match null

  section("5. Constructor / extractor patterns")
  def who(p: Person) = p match
    case Person("Ada", _)           => "Ada!"
    case Person(name, age) if age < 18 => s"minor $name"
    case Person(name, _)            => s"adult $name"
  println(who(Person("Ada", 1))); println(who(Person("Bob", 10))); println(who(Person("Cy", 30)))
  // Nested patterns
  Pair(Person("Ada", 36), Some(List(1, 2))) match
    case Pair(Person(n, _), Some(first :: _)) => println(s"  nested: $n, first=$first")
    case _                                    => ()

  section("6. Tuple patterns")
  (1, "x", true) match
    case (1, s, flag) => println(s"  tuple: $s $flag")
  // Tuples of any arity (Scala 3: also *: patterns)
  (1, 2, 3, 4) match
    case h *: tail => println(s"  head=$h tail=$tail")

  section("7. Sequence patterns")
  def seq(xs: List[Int]) = xs match
    case Nil                  => "empty"
    case List(x)              => s"one: $x"
    case List(a, b)           => s"two: $a,$b"
    case 0 :: rest            => s"starts with 0, rest=$rest"
    case List(1, _*)          => "starts with 1 (any length)"
    case first +: _ :+ last   => s"first=$first last=$last"     // works on any Seq
    case _                    => "??"
  List(Nil, List(5), List(5, 6), List(0, 9, 9), List(1, 2, 3), List(7, 8, 9)).foreach(xs => println(s"  $xs → ${seq(xs)}"))
  // Binding the vararg part:
  List(1, 2, 3, 4) match
    case List(a, rest*) => println(s"  a=$a rest=$rest")       // Scala 3 syntax (Scala 2: rest @ _*)
  // Vector / Array / any Seq works with List(...)-style? Use the companion's unapplySeq:
  Vector(1, 2) match
    case Vector(a, b) => println(s"  vector a=$a b=$b")
  Array(1, 2, 3) match
    case Array(1, more*) => println(s"  array tail=${more.toList}")

  section("8. Alternatives  p1 | p2")
  def kind(c: Char) = c match
    case 'a' | 'e' | 'i' | 'o' | 'u' => "vowel"
    case ' '                         => "space"
    case _                           => "consonant/other"
  println("hi you".map(kind).mkString(","))
  // ⚠ alternatives cannot bind variables:
  doesNotCompile("(1, 2) match { case (x, 1) | (1, x) => x }")

  section("9. Binders  name @ pattern")
  Person("Ada", 36) match
    case p @ Person(_, age) if age > 30 => println(s"  whole value $p, age $age")
    case _                               => ()
  List(1, 2, 3) match
    case all @ (first :: _) => println(s"  all=$all first=$first")
    case _                  => ()
  Option(5) match
    case s @ Some(_: Int) => println(s"  bound Some: $s")
    case _                => ()

  section("10. Guards")
  def classify(n: Int) = n match
    case x if x < 0             => "negative"
    case x if x % 2 == 0 && x > 0 => "positive even"
    case 0                      => "zero"
    case _                      => "positive odd"
  println((-1 to 3).map(classify))
  // ⚠ guards make exhaustiveness checking give up: the compiler can't reason about them

  section("11. Interpolated string patterns")
  "2026-11-03" match
    case s"$y-$m-$d" => println(s"  y=$y m=$m d=$d")
  // and regex patterns (lesson 01/04) e.g. case Date(y, m, d) =>

  section("12. Pattern definitions & generators use patterns too")
  val (a, b) = (1, 2)
  val Person(nm, _) = Person("Ann", 3)
  for (k, v) <- Map("x" -> 1) do println(s"  $k=$v")
  val x :: _ = List(1, 2): @unchecked     // refutable → needs @unchecked to silence the warning
  println(s"  $a $b $nm $x")

  section("13. Pattern-matching anonymous functions")
  val pairs = List((1, "one"), (2, "two"))
  println(pairs.map { case (n, s) => s"$s=$n" })
  println(pairs.collect { case (n, s) if n > 1 => s })

  section("14. Matching on types of Option / Either / Try")
  def opt(o: Option[Int]) = o match
    case Some(n) if n > 0 => s"positive $n"
    case Some(n)          => s"non-positive $n"
    case None             => "nothing"
  println(List(Some(1), Some(-1), None).map(opt))
  val e: Either[String, Int] = Right(3)
  e match
    case Left(err) => println(s"  error $err")
    case Right(v)  => println(s"  value $v")

  section("15. Matching on union types")
  def u(x: Int | String | Null) = x match
    case i: Int    => s"int $i"
    case s: String => s"string $s"
    case null      => "null"
  println(List[Int | String | Null](1, "a", null).map(u))

  section("16. Chained matches and `.match` (Scala 3)")
  val res = 5 match { case 5 => "five"; case _ => "other" } match { case s => s.length }
  println(res)                            // 4
  val r2 = List(1, 2).match
    case Nil    => 0
    case h :: _ => h
  println(r2)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
