//> using scala 3.9.0
// See also: lessons/04-pattern-matching/02_extractors.scala
// See also: lessons/02-functions/06_partial_functions.scala
// Run: scala-cli run lessons/04-pattern-matching/04_evaluation_order.scala
//
// Pattern evaluation order in Scala 3.9.0:
// 1. Scrutinee expression is evaluated exactly once
// 2. Extractor unapply execution across successive cases
// 3. Pattern guards are evaluated only when the preceding structural pattern matches
// 4. Short-circuiting evaluation order in alternative patterns (`case A | B`)
// 5. PartialFunction: separation between isDefinedAt check and apply evaluation

package patterns.evalorder

@main def run(): Unit =
  section("1. Scrutinee evaluation: runs exactly once")
  var scrutineeCount = 0
  def evalScrutinee(): Int =
    scrutineeCount += 1
    42

  val matchResult = evalScrutinee() match
    case 1  => "one"
    case 2  => "two"
    case 42 => "forty-two"
    case _  => "other"

  println(s"Scrutinee evaluation count: $scrutineeCount (result: $matchResult)")
  assert(scrutineeCount == 1)

  section("2. Extractor unapply calls across successive cases")
  var unapplyCount = 0
  object Halved:
    def unapply(n: Int): Option[Int] =
      unapplyCount += 1
      if n % 2 == 0 then Some(n / 2) else None

  // Scrutinee is 4. Halved.unapply(4) yields Some(2).
  // First case tests guard `half > 10` (false).
  // Second case tests guard `half == 2` (true).
  val extractorResult = 4 match
    case Halved(half) if half > 10 => s"large halved: $half"
    case Halved(half) if half == 2 => s"matched halved: $half"
    case _                         => "fallback"

  println(s"Result: $extractorResult")
  println(s"Extractor unapply count: $unapplyCount (measured in Scala 3.9.0)")
  // In Scala 3.9.0, when the same extractor appears in subsequent cases, unapply is called per case attempt
  assert(unapplyCount == 2)

  section("3. Guards evaluated only when the pattern matches")
  var guardString1Evaluated = false
  var guardIntEvaluated = false
  var guardString2Evaluated = false

  val scrutineeStr: Any = "scalagrammar"
  val guardResult = scrutineeStr match
    case s: String if { guardString1Evaluated = true; s.length < 5 } => "short string"
    case n: Int if { guardIntEvaluated = true; n > 0 }               => "positive int"
    case s: String if { guardString2Evaluated = true; s.length > 5 } => "long string"
    case _                                                           => "other"

  println(s"Guard result: $guardResult")
  println(s"Guard 1 (String): $guardString1Evaluated") // true (evaluated and failed)
  println(s"Guard 2 (Int):    $guardIntEvaluated")    // false (never called because type test failed)
  println(s"Guard 3 (String): $guardString2Evaluated") // true (evaluated and matched)
  assert(guardString1Evaluated)
  assert(!guardIntEvaluated)
  assert(guardString2Evaluated)

  section("4. Extractor in alternative patterns (short-circuiting)")
  var prefixACalls = 0
  var prefixBCalls = 0

  object StartsWithA:
    def unapply(s: String): Boolean =
      prefixACalls += 1
      s.startsWith("A")

  object StartsWithB:
    def unapply(s: String): Boolean =
      prefixBCalls += 1
      s.startsWith("B")

  val altResult = "B_token" match
    case StartsWithA() | StartsWithB() => "matches prefix A or B"
    case _                             => "other"

  println(s"Alternative pattern result: $altResult")
  println(s"StartsWithA calls: $prefixACalls") // 1 (evaluated first, false)
  println(s"StartsWithB calls: $prefixBCalls") // 1 (evaluated next, true)
  assert(prefixACalls == 1)
  assert(prefixBCalls == 1)

  section("5. PartialFunction isDefinedAt vs apply")
  // See lessons/02-functions/06_partial_functions.scala for full PartialFunction semantics.
  var bodyEvaluationCount = 0
  val pf: PartialFunction[Int, String] = {
    case n if n > 0 =>
      bodyEvaluationCount += 1
      s"Positive: $n"
  }

  // isDefinedAt checks the pattern and guard without running the case body:
  val defined = pf.isDefinedAt(10)
  println(s"pf.isDefinedAt(10): $defined, body evaluations so far: $bodyEvaluationCount")
  assert(defined)
  assert(bodyEvaluationCount == 0)

  // apply executes the case body:
  val applied = pf(10)
  println(s"pf(10): $applied, body evaluations after apply: $bodyEvaluationCount")
  assert(bodyEvaluationCount == 1)

def section(title: String): Unit = println(s"\n=== $title ===")
