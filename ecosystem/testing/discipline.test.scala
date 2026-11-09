//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
//> using test.dep org.typelevel::discipline-munit:2.0.0
//> using test.dep org.typelevel::cats-laws:2.13.0
//> using test.dep org.typelevel::cats-core:2.13.0
// See also: ecosystem/testing/munit.test.scala
// Run: scala-cli test ecosystem/testing/discipline.test.scala
//
// Type class law testing with Discipline and MUnit.
// Checks Functor laws (identity, composition) for Option with cats-laws,
// and demonstrates expected failure (.fail) on an unlawful Functor instance.

package ecosystem.testing.disciplinedemo

import cats.Functor
import cats.implicits.*
import cats.laws.discipline.FunctorTests
import munit.DisciplineSuite

case class BadBox[A](count: Int, value: A)

class DisciplineFunctorSuite extends DisciplineSuite:

  // Lawful Functor instance for Option checked against cats-laws
  checkAll("Option.FunctorLaws", FunctorTests[Option].functor[Int, Int, String])

  // Unlawful Functor instance: map breaks the identity law (fa.map(identity) != fa)
  val badFunctor: Functor[BadBox] = new Functor[BadBox]:
    def map[A, B](fa: BadBox[A])(f: A => B): BadBox[B] =
      BadBox(fa.count + 1, f(fa.value))

  test("unlawful Functor breaks identity law (expected failure)".fail) {
    val initial = BadBox(0, 42)
    val mapped = badFunctor.map(initial)(identity)
    // Fails because mapped.count is 1 while initial.count is 0.
    // Marked with .fail, so MUnit expects failure and marks the test as passing.
    assertEquals(mapped, initial)
  }
