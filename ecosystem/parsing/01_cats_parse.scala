//> using scala 3.9.0
//> using dep org.typelevel::cats-parse:1.1.0
// See also: ecosystem/lihaoyi/02_fastparse.scala
// Run: scala-cli run ecosystem/parsing/01_cats_parse.scala
//
// Parser combinators with cats-parse.
//
// Differences from fastparse:
//   - cats-parse produces an explicit, strongly-typed result (Parser[A]) without mutable state.
//   - Parser errors carry an exact failure offset (failedAtOffset) rather than a stack trace
//     or nested list of parser rule names.

package ecosystem.parsing.catsparse

import cats.parse.{Parser, Parser0}
import scala.compiletime.testing.typeCheckErrors

object PredicateGrammar:
  private val whitespace: Parser[Unit] = Parser.charIn(" \t\r\n").void
  private val whitespaces0: Parser0[Unit] = whitespace.rep0.void

  val identifier: Parser[String] =
    Parser.charsWhile(c => c.isLetterOrDigit || c == '_')

  val eqSign: Parser[Unit] =
    (whitespaces0 *> Parser.char('=') *> whitespaces0).void

  val andKeyword: Parser[Unit] =
    (whitespace.rep *> Parser.string("and") *> whitespace.rep).void

  // Single predicate: name = value
  val predicate: Parser[(String, String)] =
    (identifier ~ (eqSign *> identifier)).map((k, v) => (k, v))

  // Conjunction: name = value and name = value ...
  val query: Parser[List[(String, String)]] =
    predicate.repSep(andKeyword).map(_.toList)

@main def run(): Unit =
  section("Valid predicate parsing")
  val inputValid = "name = value and age = 30"
  val validResult = PredicateGrammar.query.parseAll(inputValid) match
    case Right(pairs) => pairs
    case Left(err)    => List.empty

  println(validResult) // List((name,value), (age,30))

  section("Invalid predicate and offset-based error reporting")
  val inputInvalid = "name ="
  val errorOutput = PredicateGrammar.query.parseAll(inputInvalid) match
    case Right(pairs) => s"unexpected success: $pairs"
    case Left(err)    =>
      // cats-parse reports the exact character offset where parsing failed:
      s"error: parse failure at offset ${err.failedAtOffset}"

  println(errorOutput) // error: parse failure at offset 6

  section("Compile-time safety with typeCheckErrors")
  // Parser returns typed Parser[A], which cannot be assigned to an arbitrary type:
  doesNotCompile("""
    val bad: Int = PredicateGrammar.identifier
  """)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
