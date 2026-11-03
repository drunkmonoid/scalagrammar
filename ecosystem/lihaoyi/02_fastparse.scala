//> using scala 3.9.0
//> using dep com.lihaoyi::fastparse:3.1.1
// Run: scala-cli run ecosystem/lihaoyi/02_fastparse.scala
//
// fastparse: parser combinators that are fast and give good error messages.
// A parser is a method `def p[$: P]: P[T]`; combine them with
//   ~ (sequence)  | (alternative)  .rep (repeat)  .? (optional)  .! (capture text)
//   .map (transform)  ~/ (cut: commit, for better errors)  Start / End  CharIn / CharsWhile

package ecosystem.lihaoyi.parsing

import fastparse.*
import NoWhitespace.*

// ---------- an arithmetic expression evaluator ----------
def number[$: P]: P[Double] = P(CharIn("0-9").rep(1).!.map(_.toDouble))
def parens[$: P]: P[Double] = P("(" ~/ addSub ~ ")")
def factor[$: P]: P[Double] = P(number | parens)
def divMul[$: P]: P[Double] = P(factor ~ (CharIn("*/").! ~/ factor).rep).map(eval)
def addSub[$: P]: P[Double] = P(divMul ~ (CharIn("+\\-").! ~/ divMul).rep).map(eval)
def expr[$: P]: P[Double]   = P(addSub ~ End)

def eval(tree: (Double, Seq[(String, Double)])): Double =
  val (base, ops) = tree
  ops.foldLeft(base) {
    case (l, ("+", r)) => l + r
    case (l, ("-", r)) => l - r
    case (l, ("*", r)) => l * r
    case (l, ("/", r)) => l / r
    case (l, _)        => l
  }

// ---------- a tiny JSON-ish key/value config format, with whitespace handling ----------
object Config:
  import fastparse.ScalaWhitespace.*                // this object's parsers skip spaces/comments
  def ident[$: P]: P[String] = P(CharsWhileIn("a-zA-Z_").!)
  def str[$: P]: P[String]   = P("\"" ~~/ CharsWhile(_ != '"', 0).! ~~ "\"")
  def num[$: P]: P[Int]      = P(CharsWhileIn("0-9").!.map(_.toInt))
  def value[$: P]: P[Any]    = P(str | num | ("[" ~/ value.rep(sep = ",") ~ "]"))
  def entry[$: P]: P[(String, Any)] = P(ident ~ "=" ~/ value)
  def file[$: P]: P[Map[String, Any]] = P(Start ~ entry.rep ~ End).map(_.toMap)

@main def run(): Unit =
  println("=== arithmetic ===")
  for input <- List("1+2*3", "(1+2)*3", "10/4-1", "2*(3+4)*5") do
    val Parsed.Success(v, _) = parse(input, expr(using _)): @unchecked
    println(f"  $input%-12s = $v")

  println("\n=== error messages ===")
  for bad <- List("1+*2", "(1+2", "12a") do
    parse(bad, expr(using _)) match
      case f: Parsed.Failure => println(f"  $bad%-6s → ${f.trace().longAggregateMsg}")
      case s                 => println(s"  unexpectedly parsed: $s")

  println("\n=== config file with whitespace & comments ===")
  val text =
    """name = "scalagrammar"   // the project
      |version = 3
      |tags = ["scala", "tutorial", 42]
      |""".stripMargin
  parse(text, Config.file(using _)) match
    case Parsed.Success(cfg, _) => cfg.toList.sortBy(_._1).foreach((k, v) => println(s"  $k → $v"))
    case f: Parsed.Failure      => println(f.trace().longAggregateMsg)
