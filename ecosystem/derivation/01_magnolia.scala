//> using scala 3.9.0
//> using dep com.softwaremill.magnolia1_3::magnolia:1.3.16
// See also: lessons/07-contextual/03_derivation.scala
// Run: scala-cli run ecosystem/derivation/01_magnolia.scala
//
// Generic type class derivation using Magnolia for Scala 3:
// 1. Defining AutoDerivation with join (product types) and split (sum types)
// 2. Deriving Show for simple case classes, nested case classes, and enums / sealed hierarchies
// 3. Comparison with hand-rolled Mirror derivation (boilerplate reduction vs manual control)

package ecosystem.derivation.magnoliademo

import magnolia1.*

// Target type class
trait Show[T]:
  def show(value: T): String

object Show extends AutoDerivation[Show]:
  // Product types (case classes, case objects)
  def join[T](ctx: CaseClass[Show, T]): Show[T] =
    new Show[T]:
      def show(value: T): String =
        val fields = ctx.params.map { param =>
          s"${param.label} = ${param.typeclass.show(param.deref(value))}"
        }
        s"${ctx.typeInfo.short}(${fields.mkString(", ")})"

  // Sum types (sealed traits, enums)
  def split[T](ctx: SealedTrait[Show, T]): Show[T] =
    new Show[T]:
      def show(value: T): String =
        ctx.choose(value) { sub =>
          sub.typeclass.show(sub.cast(value))
        }

  // Base instances
  given Show[String] = s => s"\"$s\""
  given Show[Int] = _.toString
  given Show[Double] = _.toString
  given Show[Boolean] = _.toString

// Domain ADTs for derivation verification
case class User(name: String, age: Int)

case class Address(city: String, zip: String)
case class Person(name: String, address: Address)

enum AccountStatus:
  case Active
  case Pending
  case Suspended(reason: String)

/*
 * Comparison: Hand-written Mirror Derivation vs. Magnolia:
 *
 * 1. What Magnolia's join and split replace:
 *    - In manual Mirror derivation (lessons/07-contextual/03_derivation.scala), you must write low-level
 *      tuple recursion (`summonAll`), extract field names with `constValueTuple`, cast instances to
 *      `Product`, extract elements dynamically, and manually compute `ordinal(x)` on `Mirror.SumOf`.
 *    - Magnolia's `join` automates Product inspection via `CaseClass[Typeclass, T]`, giving access to
 *      `ctx.params` with pre-summoned `param.typeclass`, `param.label`, and typed `param.deref(value)`.
 *    - Magnolia's `split` automates Sum type dispatch via `SealedTrait[Typeclass, T]`, choosing the subtype
 *      and casting safely with `ctx.choose`.
 *
 * 2. What hand-written Mirror derivation still controls:
 *    - Zero external library dependencies (pure compiler built-in `scala.deriving.Mirror`).
 *    - Exact control over compile-time code expansion and macro specialization.
 *    - Precise custom optimizations for performance-critical serialization engines.
 */

@main def run(): Unit =
  section("1. Derivation for a simple case class")
  val user = User("Ada Lovelace", 36)
  val userShow = summon[Show[User]].show(user)
  println(s"User Show: $userShow")
  assert(userShow == "User(name = \"Ada Lovelace\", age = 36)")

  section("2. Derivation for a nested case class")
  val person = Person("Grace Hopper", Address("Arlington", "22201"))
  val personShow = summon[Show[Person]].show(person)
  println(s"Person Show: $personShow")
  assert(personShow == "Person(name = \"Grace Hopper\", address = Address(city = \"Arlington\", zip = \"22201\"))")

  section("3. Derivation for enum / sealed trait (sum types)")
  val active: AccountStatus = AccountStatus.Active
  val suspended: AccountStatus = AccountStatus.Suspended("Policy violation")

  val activeShow = summon[Show[AccountStatus]].show(active)
  val suspendedShow = summon[Show[AccountStatus]].show(suspended)

  println(s"Active Show:    $activeShow")
  println(s"Suspended Show: $suspendedShow")
  assert(activeShow == "Active()")
  assert(suspendedShow == "Suspended(reason = \"Policy violation\")")

  println("\nMagnolia derivation completed and verified successfully.")

def section(title: String): Unit = println(s"\n=== $title ===")
