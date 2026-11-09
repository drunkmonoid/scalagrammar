//> using scala 3.9.0
// See also: lessons/01-basics/03_types_literals.scala
// Run: scala-cli run lessons/11-advanced/16_numeric_tower.scala
//
// The Scala Numeric Tower & Floating Point Semantics:
// 1. Generic arithmetic with Numeric, Integral, and Fractional (integer truncation in average)
// 2. Exact decimal arithmetic with BigDecimal and RoundingMode policies
// 3. Double.toString round-trip representation (shortest decimal representation)
// 4. Thread-safe Locale-specific formatting (Locale.GERMANY vs Locale.US)
// 5. Total ordering vs IEEE 754 comparisons on Double.NaN

package advanced.numerictower

import scala.math.BigDecimal.RoundingMode
import java.util.Locale

def genericSum[T](xs: List[T])(using num: Numeric[T]): T =
  xs.foldLeft(num.zero)(num.plus)

def averageIntegral[T](xs: List[T])(using integ: Integral[T]): T =
  integ.quot(genericSum(xs), integ.fromInt(xs.size))

def averageFractional[T](xs: List[T])(using frac: Fractional[T]): T =
  frac.div(genericSum(xs), frac.fromInt(xs.size))

@main def run(): Unit =
  section("1. Generic arithmetic: Numeric, Integral, and Fractional")
  val ints = List(1, 2)
  val doubles = List(1.0, 2.0)

  val intSum = genericSum(ints)
  val dblSum = genericSum(doubles)
  println(s"Sum of ints: $intSum, sum of doubles: $dblSum")
  assert(intSum == 3)
  assert(dblSum == 3.0)

  // ⚠ Integral.quot performs integer division and truncates fractional parts:
  val intAvg = averageIntegral(ints)
  val dblAvg = averageFractional(doubles)
  println(s"averageIntegral(List(1, 2)):   $intAvg (truncated integer division!)")
  println(s"averageFractional(List(1.0, 2.0)): $dblAvg (exact floating division)")
  assert(intAvg == 1)
  assert(dblAvg == 1.5)

  section("2. BigDecimal rounding modes on the same value (2.555)")
  val base = BigDecimal("2.555")
  val modes = List(
    RoundingMode.UP,
    RoundingMode.DOWN,
    RoundingMode.CEILING,
    RoundingMode.FLOOR,
    RoundingMode.HALF_UP,
    RoundingMode.HALF_DOWN,
    RoundingMode.HALF_EVEN
  )

  println(s"Original value: $base")
  for mode <- modes do
    val rounded = base.setScale(2, mode)
    println(f"  $mode%-10s -> $rounded")

  assert(base.setScale(2, RoundingMode.UP) == BigDecimal("2.56"))
  assert(base.setScale(2, RoundingMode.DOWN) == BigDecimal("2.55"))
  assert(base.setScale(2, RoundingMode.HALF_UP) == BigDecimal("2.56"))
  assert(base.setScale(2, RoundingMode.HALF_EVEN) == BigDecimal("2.56"))

  section("3. Double.toString round-trip precision")
  // Double.toString in modern JVMs produces the shortest decimal representation
  // that round-trips uniquely back to the exact IEEE 754 binary floating-point representation.
  val d1 = 0.1
  val d2 = 1e21
  val d3 = 1e-7

  println(s"0.1:  ${d1.toString} (round-trips: ${java.lang.Double.parseDouble(d1.toString) == d1})")
  println(s"1e21: ${d2.toString} (round-trips: ${java.lang.Double.parseDouble(d2.toString) == d2})")
  println(s"1e-7: ${d3.toString} (round-trips: ${java.lang.Double.parseDouble(d3.toString) == d3})")
  assert(java.lang.Double.parseDouble(d1.toString) == d1)
  assert(java.lang.Double.parseDouble(d2.toString) == d2)
  assert(java.lang.Double.parseDouble(d3.toString) == d3)

  section("4. Locale formatting (US vs GERMANY) without altering process default")
  val sampleAmount = 1234567.89
  val usFormat = String.format(Locale.US, "%,.2f", sampleAmount)
  val deFormat = String.format(Locale.GERMANY, "%,.2f", sampleAmount)

  println(s"Locale.US:      $usFormat") // "1,234,567.89"
  println(s"Locale.GERMANY: $deFormat") // "1.234.567,89"
  assert(usFormat == "1,234,567.89")
  assert(deFormat == "1.234.567,89")

  section("5. Ordering[Double] TotalOrdering vs IEEE 754 comparisons on NaN")
  val nan = Double.NaN
  // Under IEEE 754, NaN is not equal to anything, including itself:
  println(s"IEEE 754 comparison (nan == nan): ${nan == nan}") // false
  assert(!(nan == nan))

  // In Scala, Ordering[Double] provides a total order where NaN == NaN and NaN > +Infinity:
  val doubleOrdering = summon[Ordering[Double]]
  val nanCmp = doubleOrdering.compare(nan, nan)
  println(s"Ordering[Double].compare(nan, nan): $nanCmp") // 0
  assert(nanCmp == 0)

  val unsorted = List(nan, 1.0, Double.PositiveInfinity, -5.0)
  val sorted = unsorted.sorted
  println(s"Sorted numbers with total ordering: $sorted")
  // In total ordering: -5.0 < 1.0 < +Infinity < NaN
  assertEquals(sorted.last.isNaN, true)
  assertEquals(sorted.head, -5.0)

def assertEquals(actual: Any, expected: Any): Unit =
  assert(actual == expected, s"Expected $expected but got $actual")

def section(title: String): Unit = println(s"\n=== $title ===")
