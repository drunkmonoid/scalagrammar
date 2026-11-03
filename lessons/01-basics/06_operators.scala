//> using scala 3.9.0
// Run: scala-cli run lessons/01-basics/06_operators.scala
//
// Operators are just methods. `a + b` is `a.+(b)`. The rules that govern them:
//   * precedence is decided by the FIRST character of the operator
//   * associativity is decided by the LAST character (`:` → right-associative)
//   * operators ending in `=` (except comparisons) are assignment operators
//   * prefix operators: only + - ! ~ via `unary_` methods

package basics.operators

import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("Operators are methods")
  println(1 + 2)
  println(1.+(2))                  // same thing
  println(List(1, 2).++(List(3)))
  val s = "hello"
  println(s indexOf 'l')           // alphanumeric infix call — see `infix` below
  println(s.charAt(1))

  section("Precedence (lowest → highest, by FIRST character)")
  //   (all letters)
  //   |
  //   ^
  //   &
  //   = !
  //   < >
  //   :
  //   + -
  //   * / %
  //   (all other special characters, e.g. #, ?, @, \, ~)
  println(2 + 3 * 4)               // 14
  println(1 + 2 :: Nil)            // List(3) — `+` binds tighter than `::`
  println(true || false && false)  // true — && (&) binds tighter than || (|)
  println(1 < 2 == true)           // `<` binds tighter than `==`
  // ⚠ alphanumeric operators have the LOWEST precedence:
  println(1 + 2 max 2 + 1)         // (1+2) max (2+1) = 3
  // ⚠ a custom operator starting with `*` is "multiplicative":
  case class V(x: Int):
    def +(o: V) = V(x + o.x)
    def *+*(o: V) = V(x * o.x)
  println(V(1) + V(2) *+* V(3))    // V(1) + (V(2) *+* V(3)) = V(7)

  section("Associativity (by LAST character)")
  // Operators ending in ':' are right-associative AND are invoked on the RIGHT operand:
  //   a :: b  ==  b.::(a)
  val l = 1 :: 2 :: 3 :: Nil       // 1 :: (2 :: (3 :: Nil))
  println(l)
  println(0 +: l :+ 4)             // `+:` starts with '+' → binds tighter than `:+`: (0 +: l) :+ 4
  case class Stack(xs: List[Int]):
    def >>:(x: Int): Stack = Stack(x :: xs)
  println(1 >>: 2 >>: Stack(Nil))  // Stack(List(1, 2)) — evaluated as Stack(Nil).>>:(2).>>:(1)
  // ⚠ Mixing left- and right-associative operators of the SAME precedence is an error.
  // `:+` and `::` both start with ':' but `:+` is left-assoc and `::` right-assoc:
  doesNotCompile("List(1) :+ 2 :: Nil")

  section("Assignment operators")
  var n = 1
  n += 2                           // n = n + 2 (Int has no += method)
  n *= 10
  println(n)                       // 30
  val buf = scala.collection.mutable.ListBuffer(1)
  buf += 2                         // real method ListBuffer.+=
  println(buf)
  var imm = List(1)
  imm :+= 2                        // imm = imm :+ 2  (works for any op if a var)
  imm ::= 0                        // imm = 0 :: imm  ⚠ right-assoc respected
  println(imm)
  // ⚠ <=, >=, != are comparisons, not assignment operators.

  section("Prefix (unary) operators")
  case class Money(cents: Long):
    def unary_- : Money = Money(-cents)   // note the space before ':'
    def unary_! : Boolean = cents == 0
  println(-Money(5))
  println(!Money(0))
  doesNotCompile("case class W(i: Int) { def unary_* = 1 }; *W(1)")   // only + - ! ~

  section("Postfix operators — off by default")
  doesNotCompile("List(1, 2) size")
  // import scala.language.postfixOps enables them. Avoid: they confuse semicolon inference.

  section("Alphanumeric infix methods and @infix")
  // In Scala 3, calling a method with letters infix (`a max b`) warns (under -source future)
  // unless the method is marked `infix`.
  case class Vec(x: Int, y: Int):
    infix def dot(o: Vec): Int = x * o.x + y * o.y
  println(Vec(1, 2) dot Vec(3, 4)) // 11
  // `infix` types too (defined at the bottom of the file):
  val e: Int or String = Left(1)
  println(e)

  section("== vs equals vs eq")
  // ==   null-safe, calls equals (with numeric cooperation)
  // eq   reference identity (AnyRef only)
  // equals overridable structural equality
  val a = List(1, 2)
  val b = List(1, 2)
  println(a == b)                  // true
  println(a eq b)                  // false
  val nul: List[Int] | Null = null
  println(nul == a)                // false, no NPE
  doesNotCompile("1 eq 1")         // eq is not defined on Int (AnyVal)
  // != is !(a == b); ne is !(a eq b)

  section("Operators on Boolean, and their method names")
  println(true.&&(false))
  println(true ^ true)             // XOR → false

  section("apply / update — the invisible operators")
  object Square:
    def apply(n: Int): Int = n * n
  println(Square(4))               // Square.apply(4)
  val arr = Array(1, 2, 3)
  arr(0) = 10                      // arr.update(0, 10)
  println(arr.toList)
  class Grid:
    private val cells = Array.ofDim[Int](3, 3)
    def apply(r: Int, c: Int): Int = cells(r)(c)
    def update(r: Int, c: Int, v: Int): Unit = cells(r)(c) = v
  val g = Grid()
  g(1, 2) = 7                      // g.update(1, 2, 7)
  println(g(1, 2))

  section("Symbolic method names under the hood (name mangling)")
  println(classOf[V].getMethods.map(_.getName).filter(_.startsWith("$")).sorted.toList)
  // `+` → $plus, `*+*` → $times$plus$times, `::` → $colon$colon

  section("Semicolon inference and operators at line ends")
  val total = 1 +
    2                              // operator at end of line: continues
  println(total)
  val total2 = 1
    + 2                            // Scala 3: leading infix operator continues the expression
  println(total2)                  // 3
  // ⚠ in Scala 2 the second form was two statements: `1` and `+2`.

infix type or[A, B] = Either[A, B]

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("compiles (no error)")}")
