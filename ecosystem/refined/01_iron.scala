//> using scala 3.9.0
//> using dep io.github.iltotore::iron:3.4.1
// Run: scala-cli run ecosystem/refined/01_iron.scala
//
// Iron: refined types for Scala 3. A type like `Int :| Positive` is an Int that is PROVEN to be
// positive. Constants are checked at COMPILE time (via inline); runtime values go through
// refineEither / refineOption / refineUnsafe. At runtime it's just the plain value (opaque type).
// (Scala 2 had `refined`; Iron is its Scala 3 successor built on inline + opaque types.)

package ecosystem.refined.irondemo

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import scala.compiletime.testing.typeCheckErrors

// Named constraints and new types
type Age = Int :| Interval.Closed[0, 150]
type Username = String :| (MinLength[3] & MaxLength[16] & Match["[a-z][a-z0-9_]*"])
type Percentage = Double :| Interval.Closed[0.0, 100.0]

// RefinedType companions give a distinct type with helpers (apply/either/option)
type Email = Email.T
object Email extends RefinedType[String, Match["[^@\\s]+@[^@\\s]+\\.[a-z]+"]]

case class User(name: Username, age: Age, email: Email)

def averageAge(users: List[User]): Double = users.map(_.age.toDouble).sum / users.size

@main def run(): Unit =
  println("=== compile-time checked constants ===")
  val age: Age = 36                              // checked by the compiler
  val name: Username = "ada_lovelace"
  val pct: Percentage = 99.5
  println(s"  $age $name $pct")
  for err <- typeCheckErrors("val bad: ecosystem.refined.irondemo.Age = 200") do
    println(s"  ✗ val bad: Age = 200 → ${err.message.linesIterator.filter(_.nonEmpty).take(2).mkString(" / ")}")
  for err <- typeCheckErrors("""val bad: ecosystem.refined.irondemo.Username = "X" """) do
    println(s"  ✗ val bad: Username = \"X\" → ${err.message.linesIterator.filter(_.nonEmpty).take(2).mkString(" / ")}")

  println("\n=== runtime values: refine with Either / Option ===")
  val inputs = List("ada" -> 36, "BOB" -> 41, "cy" -> 200)
  for (n, a) <- inputs do
    val user = for
      nn <- n.refineEither[MinLength[3] & MaxLength[16] & Match["[a-z][a-z0-9_]*"]]
      aa <- a.refineEither[Interval.Closed[0, 150]]
      ee <- Email.either(s"$n@example.org")
    yield User(nn, aa, ee)
    println(s"  ($n, $a) → $user")
  println(s"  refineOption: ${(-5).refineOption[Positive]} ${5.refineOption[Positive]}")
  println(s"  Email.option: ${Email.option("nope")} ${Email.option("a@b.io")}")

  println("\n=== refined values ARE their base type at runtime ===")
  val users = List(User("ada", 36, Email("ada@x.org")), User("bob", 41, Email("bob@x.org")))
  println(s"  average age: ${averageAge(users)}")
  val raw: Int = age                             // a refined Int widens to Int
  println(s"  as plain Int: ${raw + 1}")
  println(s"  runtime class: ${(age: Any).getClass.getSimpleName}")   // Integer: no wrapper

  println("\n=== why bother? ===")
  // Validation happens ONCE at the boundary (parsing input); everything downstream receives
  // types that make invalid states unrepresentable, e.g. averageAge never sees age = -1,
  // and a function taking `Int :| Positive` documents and enforces its precondition.
  println("  see comments")
