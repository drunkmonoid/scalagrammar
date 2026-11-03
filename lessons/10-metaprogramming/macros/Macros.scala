//> using scala 3.9.0
// Run the whole directory:  scala-cli run lessons/10-metaprogramming/macros/
//
// Quoted macros: code that runs INSIDE the compiler and returns code.
//   '{ ... }   quote:  turns code into an Expr[T] (a typed syntax tree)
//   ${ ... }   splice: evaluates an Expr-producing computation and inserts its result
//   Type[T]    the type-level counterpart of Expr
//   Quotes     the compiler context; quotes.reflect gives full TASTy tree access
//
// ⚠ A macro must be compiled BEFORE the code that calls it. Keeping the definitions in this
//   file and the usages in Main.scala lets the compiler order them (it suspends Main until
//   Macros is compiled). Calling a macro from the same file that defines it fails.

package meta.macros

import scala.quoted.*

// ---------- 1. debug: print an expression's source together with its value ----------
inline def debug(inline expr: Any): Unit = ${ debugImpl('expr) }

def debugImpl(expr: Expr[Any])(using Quotes): Expr[Unit] =
  val source = Expr(expr.show)                // the code, as text, computed at compile time
  '{ println("  " + $source + " = " + $expr) }

// ---------- 2. compile-time validation of literal arguments ----------
inline def port(inline n: Int): Int = ${ portImpl('n) }

def portImpl(n: Expr[Int])(using Quotes): Expr[Int] =
  val value = n.valueOrAbort                  // aborts with an error if n isn't a literal
  if value < 1 || value > 65535 then
    quotes.reflect.report.errorAndAbort(s"invalid port $value (must be 1..65535)", n)
  n

// ---------- 3. code generation: unrolled exponentiation ----------
inline def pow(x: Double, inline n: Int): Double = ${ powImpl('x, 'n) }

def powImpl(x: Expr[Double], n: Expr[Int])(using Quotes): Expr[Double] =
  def go(k: Int): Expr[Double] =
    if k == 0 then '{ 1.0 }
    else '{ $x * ${ go(k - 1) } }
  go(n.valueOrAbort)

inline def powCode(x: Double, inline n: Int): String = ${ Expr(powImpl('x, 'n).show) }

// ---------- 4. quote pattern matching: a tiny optimiser ----------
inline def simplify(inline e: Int): Int = ${ simplifyImpl('e) }
inline def simplifyCode(inline e: Int): String = ${ Expr(simplifyImpl('e).show) }

def simplifyImpl(e: Expr[Int])(using Quotes): Expr[Int] = e match
  case '{ ($x: Int) * 1 }         => simplifyImpl(x)
  case '{ 1 * ($x: Int) }         => simplifyImpl(x)
  case '{ ($x: Int) + 0 }         => simplifyImpl(x)
  case '{ ($x: Int) * 0 }         => '{ 0 }
  case '{ ($a: Int) + ($b: Int) } => '{ ${ simplifyImpl(a) } + ${ simplifyImpl(b) } }
  case _                          => e

// ---------- 5. working with types ----------
inline def typeName[T]: String = ${ typeNameImpl[T] }

def typeNameImpl[T: Type](using Quotes): Expr[String] = Expr(Type.show[T])

inline def isCaseClass[T]: Boolean = ${ isCaseClassImpl[T] }

def isCaseClassImpl[T: Type](using Quotes): Expr[Boolean] =
  import quotes.reflect.*
  Expr(TypeRepr.of[T].typeSymbol.flags.is(Flags.Case))

// ---------- 6. TASTy reflection: inspect a class's structure ----------
inline def fieldNames[T]: List[String] = ${ fieldNamesImpl[T] }

def fieldNamesImpl[T: Type](using Quotes): Expr[List[String]] =
  import quotes.reflect.*
  Expr(TypeRepr.of[T].typeSymbol.caseFields.map(_.name))

// Generate a toMap for any case class: field name → value
inline def toMap[T <: Product](t: T): Map[String, Any] = ${ toMapImpl('t) }

def toMapImpl[T <: Product: Type](t: Expr[T])(using Quotes): Expr[Map[String, Any]] =
  import quotes.reflect.*
  val pairs: List[Expr[(String, Any)]] =
    TypeRepr.of[T].typeSymbol.caseFields.map { field =>
      val value = Select(t.asTerm, field).asExprOf[Any]       // t.field
      '{ (${ Expr(field.name) }, $value) }
    }
  '{ Map(${ Varargs(pairs) }*) }

// ---------- 7. source positions ----------
inline def here: String = ${ hereImpl }

def hereImpl(using Quotes): Expr[String] =
  import quotes.reflect.*
  val pos = Position.ofMacroExpansion
  Expr(s"${pos.sourceFile.name}:${pos.startLine + 1}")

// ---------- 8. look at the raw tree ----------
inline def treeOf(inline e: Any): String = ${ treeOfImpl('e) }

def treeOfImpl(e: Expr[Any])(using Quotes): Expr[String] =
  import quotes.reflect.*
  Expr(e.asTerm.show(using Printer.TreeStructure))
