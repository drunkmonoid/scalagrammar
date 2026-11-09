//> using scala 3.9.0
// See also: lessons/02-functions/01_methods.scala
// See also: lessons/06-types/01_type_hierarchy.scala
// Run: scala-cli run lessons/11-advanced/11_overloading_inference.scala
//
// Overloading resolution and type inference in Scala 3:
// 1. Parameter type propagation to lambdas across overloaded alternatives
// 2. Missing expected type on lambdas (e.g. `x => x`)
// 3. Ambiguity between SAM types like Runnable and Callable[T]
// 4. Most-specific alternative selection across subtyping hierarchy
// 5. Default argument restrictions in overloaded methods
// 6. Left-to-right type inference flow across multiple parameter lists

package advanced.overloading

import scala.compiletime.testing.typeCheckErrors
import java.util.concurrent.Callable

// Section 1: Overloaded methods where parameter types disagree
object OverloadedLambda:
  def process(f: String => Int): Int = f("hello")
  def process(f: Int => Int): Int = f(42)

// Section 3: Ambiguous SAM alternatives (Runnable vs Callable)
object SamAmbiguity:
  def submit(r: Runnable): String = "Runnable"
  def submit[T](c: Callable[T]): String = "Callable"

// Section 4: Most specific alternative selection
object Specificity:
  def inspect(x: Int): String = s"Int: $x"
  def inspect(x: Long): String = s"Long: $x"
  def inspect(x: Double): String = s"Double: $x"
  def inspect(x: Any): String = s"Any: $x"

@main def run(): Unit =
  section("1. Lambda parameter type agreement across overloaded alternatives")
  // When overloaded alternatives have different parameter types (String vs Int),
  // a lambda without explicit parameter types cannot infer its type.
  val lambdaErrors = typeCheckErrors("advanced.overloading.OverloadedLambda.process(x => x.hashCode)")
  println(s"Error when parameter types disagree: ${lambdaErrors.head.message}")

  section("2. Lambda with no expected type (x => x)")
  // `x => x` requires a target expected type; without it, the compiler cannot typecheck the lambda.
  val noExpectedTypeErrors = typeCheckErrors("val f = (x => x)")
  println(s"Error without expected type: ${noExpectedTypeErrors.head.message}")

  section("3. Ambiguity between Runnable and Callable[T]")
  // A Unit-returning lambda is ambiguous because () => Unit matches Runnable.run(): Unit
  // and Callable[Unit].call(): Unit (or Callable[Object] via value boxing).
  val samErrors = typeCheckErrors("advanced.overloading.SamAmbiguity.submit(() => println(\"task\"))")
  println(s"Ambiguous SAM call error: ${samErrors.head.message}")
  // Fix 1: Explicit type ascription to Runnable
  val fixedWithAscription = SamAmbiguity.submit((() => println("task")): Runnable)
  println(s"Resolved with ascription: $fixedWithAscription")
  // Fix 2: Explicit SAM constructor
  val fixedWithSam = SamAmbiguity.submit(Runnable(() => println("task")))
  println(s"Resolved with SAM constructor: $fixedWithSam")

  section("4. Overloading picks the most specific alternative")
  // Char widens to Int, Long, and Double; Int is the most specific subtype in the numeric hierarchy.
  val charResult = Specificity.inspect('A')
  println(s"inspect('A') resolved to -> $charResult")
  assert(charResult.startsWith("Int: 65"))

  // Long argument matches Long directly without widening to Double or Any.
  val longResult = Specificity.inspect(100L)
  println(s"inspect(100L) resolved to -> $longResult")
  assert(longResult.startsWith("Long: 100"))

  section("5. At most one overloaded alternative may have default arguments")
  // Having default arguments on more than one alternative in an overloaded method family is a compile error.
  val defaultArgSnippet =
    """class BadOverload {
      |  def compute(x: Int = 1): Int = x
      |  def compute(x: Int, y: Int = 2): Int = x + y
      |}""".stripMargin
  val defaultErrors = typeCheckErrors(defaultArgSnippet)
  println(s"Multiple defaults error: ${defaultErrors.head.message}")

  section("6. Left-to-right inference flow across parameter lists")
  // In `List.foldLeft(z)(op)`, type inference infers the accumulator type B from `z`.
  // When passing `Nil` directly, B is inferred as `Nil.type`.
  // In the second argument list `(acc, x) => x :: acc`, the result is `List[Int]`,
  // which does not conform to `Nil.type`.
  val nilFoldError = typeCheckErrors("List(1, 2, 3).foldLeft(Nil)((acc, x) => x :: acc)")
  println(s"foldLeft(Nil) error: ${nilFoldError.head.message}")

  // When passing `List.empty[Int]`, B is inferred as `List[Int]`, and inference succeeds:
  val reversed = List(1, 2, 3).foldLeft(List.empty[Int])((acc, x) => x :: acc)
  println(s"foldLeft(List.empty[Int]) succeeded: $reversed")
  assert(reversed == List(3, 2, 1))

def section(title: String): Unit = println(s"\n=== $title ===")
