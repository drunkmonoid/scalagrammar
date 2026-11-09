//> using scala 3.9.0
//> using dep dev.optics::monocle-core:3.3.0
//> using dep dev.optics::monocle-macro:3.3.0
// Run: scala-cli run ecosystem/optics/01_monocle.scala
//
// Functional optics with Monocle: composable Lenses for nested immutability and Optionals.

package ecosystem.optics.monocledemo

import monocle.{Lens, Optional}
import scala.compiletime.testing.typeCheckErrors

// Nested case classes (two levels deep)
case class Street(name: String, zipCode: Int)
case class Address(city: String, street: Street)
case class Person(name: String, address: Address)

// Data structure with an Option field
case class Account(id: String, limit: Option[Int])

@main def run(): Unit =
  section("Nested case class update via composed Lens")
  val personAddress = Lens[Person, Address](_.address)(a => p => p.copy(address = a))
  val addressStreet = Lens[Address, Street](_.street)(s => a => a.copy(street = s))
  val streetZip = Lens[Street, Int](_.zipCode)(z => s => s.copy(zipCode = z))

  // Composed lens focusing two levels deep onto an Int field:
  val personZip: Lens[Person, Int] = personAddress.andThen(addressStreet).andThen(streetZip)

  val initialPerson = Person("Ada", Address("London", Street("Baker St", 10001)))
  val updatedPerson = personZip.replace(90210)(initialPerson)

  val updatedZip = personZip.get(updatedPerson)
  println(updatedZip) // 90210

  section("Optional / Prism over an Option field")
  val accountLimit: Optional[Account, Int] = Optional[Account, Int](_.limit)(newLimit =>
    acc => acc.copy(limit = Some(newLimit))
  )

  // 1. Modifying Some
  val withSome = Account("acc-1", Some(100))
  val modifiedSome = accountLimit.modify(_ * 2)(withSome)
  println(modifiedSome.limit) // Some(200)

  // 2. Modifying None: remains unchanged
  val withNone = Account("acc-2", None)
  val unchangedNone = accountLimit.modify(_ * 2)(withNone)
  println(unchangedNone.limit) // None
  println(unchangedNone)       // Account(acc-2,None)

  section("Compile-time safety with typeCheckErrors")
  // Replacing with an incompatible type fails at compile time:
  doesNotCompile("""
    val p = Person("Ada", Address("London", Street("Baker St", 10001)))
    personZip.replace("not-an-int")(p)
  """)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
