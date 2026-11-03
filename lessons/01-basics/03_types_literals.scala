//> using scala 3.9.0
// Run: scala-cli run lessons/01-basics/03_types_literals.scala
//
// Primitive ("value") types, literals, numeric conversions and their traps.

package basics.literals

import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("The nine AnyVal types")
  println(s"Byte    ${Byte.MinValue} .. ${Byte.MaxValue}")
  println(s"Short   ${Short.MinValue} .. ${Short.MaxValue}")
  println(s"Int     ${Int.MinValue} .. ${Int.MaxValue}")
  println(s"Long    ${Long.MinValue} .. ${Long.MaxValue}")
  println(s"Float   ${Float.MinPositiveValue} .. ${Float.MaxValue}")
  println(s"Double  ${Double.MinPositiveValue} .. ${Double.MaxValue}")
  println(s"Char    ${Char.MinValue.toInt} .. ${Char.MaxValue.toInt} (UTF-16 code unit)")
  println("Boolean true/false;  Unit has exactly one value: ()")

  section("Integer literals")
  println(1_000_000)              // underscores as separators
  println(0xFF)                   // hex → 255
  println(0b1010)                 // binary → 10
  println(100L)                   // Long; lowercase 'l' is deprecated (looks like 1)
  println(012)                    // ⚠ 12 — NOT octal (Java/C would give 10); avoid leading zeros
  doesNotCompile("val i: Int = 3000000000")  // too large for Int

  section("Floating literals")
  println(1.5)                    // Double
  println(1.5f)                   // Float
  println(1e3)                    // 1000.0
  println(.5)                     // 0.5
  println(1d)                     // 1.0
  val narrowed: Float = 1.1       // a Double LITERAL is narrowed to Float when the type asks for it
  println(narrowed)
  doesNotCompile("val d = 1.5; val f: Float = d")   // but a Double VALUE is never narrowed

  section("Char and Boolean literals")
  println('a')
  println('A')               // A
  println('\n'.toInt)             // 10
  println('a' + 1)                // ⚠ 98 — Char + Int is Int arithmetic
  println(('a' + 1).toChar)       // b

  section("Integer arithmetic traps")
  println(7 / 2)                  // 3     integer division truncates toward zero
  println(-7 / 2)                 // -3
  println(-7 % 2)                 // -1    remainder takes the sign of the dividend
  println(Math.floorMod(-7, 2))   // 1     mathematical modulo
  println(Int.MaxValue + 1)       // ⚠ -2147483648  silent overflow (wraps)
  println(Math.addExact(1, 2))
  try Math.addExact(Int.MaxValue, 1)
  catch case e: ArithmeticException => println(s"addExact: $e")
  println(Int.MinValue.abs)       // ⚠ -2147483648  abs overflows too
  try println(1 / 0)
  catch case e: ArithmeticException => println(s"1 / 0 → $e")
  println(1.0 / 0)                // Infinity (floats do not throw)
  println(0.0 / 0)                // NaN

  section("Floating point traps")
  println(0.1 + 0.2)              // 0.30000000000000004
  println(0.1 + 0.2 == 0.3)       // false
  println(math.abs(0.1 + 0.2 - 0.3) < 1e-9)
  println(BigDecimal("0.1") + BigDecimal("0.2") == BigDecimal("0.3"))   // true
  val nan = Double.NaN
  println(nan == nan)             // ⚠ false (IEEE 754)
  println(nan.isNaN)              // true
  println(-0.0 == 0.0)            // true
  println((-0.0).equals(0.0))     // ⚠ false — boxed equals distinguishes signed zero
  println(1.0 / -0.0)             // -Infinity
  println(16777217f)              // ⚠ 1.6777216E7 — Float has 24 bits of mantissa
  println(math.round(2.5))        // 3
  println(math.round(-2.5))       // ⚠ -2  (rounds half up toward +∞)
  println(BigDecimal(2.5).setScale(0, BigDecimal.RoundingMode.HALF_EVEN))  // 2 (banker's)

  section("Numeric widening (implicit, safe-ish)")
  val b: Byte = 10
  val s: Short = b                // Byte → Short
  val i: Int = s                  // Short → Int
  val l: Long = i
  val f: Float = l                // ⚠ allowed but lossy for big Longs (deprecated warnings)
  val d: Double = f
  println(s"$b $s $i $l $f $d")
  println(123456789.toFloat)      // ⚠ 1.23456792E8 — precision loss
  // Literal narrowing: an Int *literal* fits in Byte/Short/Char if in range
  val byte: Byte = 127
  doesNotCompile("val bad: Byte = 128")

  section("Explicit conversions (narrowing truncates bits)")
  println(300.toByte)             // 44   (300 & 0xFF, then signed)
  println(3.99.toInt)             // 3    (truncate)
  println(-3.99.toInt)            // -3
  println(1e20.toInt)             // 2147483647 (saturates for Double → Int)
  println(Double.NaN.toInt)       // 0
  println("42".toInt + "1.5".toDouble)
  println("x".toIntOption)        // None — safe parsing
  try "x".toInt catch case e: NumberFormatException => println(e)

  section("Arbitrary precision")
  val big = BigInt(2).pow(100)
  println(big)
  println(BigInt("123456789012345678901234567890") * 2)
  println(BigDecimal(1) / 3)      // 0.3333333333333333333333333333333333 (MathContext.DECIMAL128)
  println(BigDecimal(0.1))        // ⚠ 0.1 — Scala's BigDecimal(Double) uses Double.toString
  println(new java.math.BigDecimal(0.1)) // Java's: 0.1000000000000000055511151231257827...

  section("Bitwise operations")
  println(5 & 3)                  // 1
  println(5 | 3)                  // 7
  println(5 ^ 3)                  // 6
  println(~5)                     // -6
  println(1 << 4)                 // 16
  println(-16 >> 2)               // -4  (arithmetic shift, keeps sign)
  println(-16 >>> 28)             // 15  (logical shift, fills zeros)
  println(1 << 33)                // ⚠ 2 — shift distance is taken mod 32 for Int
  println(Integer.toBinaryString(10))
  println(java.lang.Long.bitCount(255L))

  section("Rich operations (via implicit conversion to RichInt etc.)")
  println(5.max(9))
  println((-5).abs)
  println(1 to 5)
  println(1 until 5)
  println(10.toHexString)
  println(3.14.round)
  println(10.isValidByte)
  println('x'.isLetter)
  println(255.toBinaryString)

  section("Boxing")
  // Int values are unboxed JVM ints, but generic code sees java.lang.Integer.
  val boxed: Any = 1
  println(boxed.getClass)         // class java.lang.Integer
  val list = List(1, 2, 3)        // stored boxed inside List
  println(list.head.getClass)     // int (the static type at the call site is Int)
  // ⚠ Java Integer cache: reference equality on boxed values -128..127 is "true"
  val a: AnyRef = Integer.valueOf(127)
  val a2: AnyRef = Integer.valueOf(127)
  val c: AnyRef = Integer.valueOf(1000)
  val c2: AnyRef = Integer.valueOf(1000)
  println(s"127 eq 127: ${a eq a2},  1000 eq 1000: ${c eq c2}")  // true, false
  println(s"but == compares values: ${c == c2}")                  // true
  // Scala's == on numbers is cooperative across types:
  println(1 == 1L)                // true
  println(1 == 1.0)               // true
  println((1: Any) == (1L: Any))  // true  (Scala's BoxesRunTime.equals)
  println((1: Any).equals(1L))    // ⚠ false (Java equals on Integer vs Long)
  println(1.## == 1L.##)          // true — ## is the hash consistent with ==
  println(1.hashCode == 1.0.hashCode) // ⚠ false

  section("Literal types / singleton types (preview — see lessons/06-types)")
  val one: 1 = 1
  doesNotCompile("val one: 1 = 2")
  val yes: true = true
  println(s"$one $yes")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
