//> using scala 3.9.0
//> using dep org.typelevel::cats-core:2.13.0
//> using dep org.typelevel::kittens:3.5.0
// Run: scala-cli run ecosystem/typelevel/01_cats_typeclasses.scala
//
// cats: the standard library of functional programming in Scala. It provides
//   * type classes:  Eq, Show, Order, Semigroup, Monoid, Functor, Applicative, Monad, Traverse...
//   * instances for standard types (Int, String, List, Option, Either, Map, Future, ...)
//   * syntax (extension methods):  import cats.syntax.all.*
//   * data types: NonEmptyList, Validated, Chain, Ior, Kleisli, State... (next lesson)
// kittens derives cats type classes for your own case classes / enums.

package ecosystem.cats.typeclasses

import cats.*
import cats.syntax.all.*
import cats.derived.*

case class Money(cents: Long) derives Eq, Show, Order
case class Stats(count: Int, total: Long, tags: Set[String]) derives Monoid

@main def run(): Unit =
  section("Eq: type-safe equality")
  println(1 === 1)
  println("a" =!= "b")
  println(Money(1) === Money(1))
  // 1 === "1" does not compile: Eq needs both sides of the same type
  println(scala.compiletime.testing.typeCheckErrors("import cats.syntax.all.*; 1 === \"1\"").map(_.message.linesIterator.next()))

  section("Show: a toString you control")
  println(Money(250).show)
  println(List(1, 2).show)
  println(show"interpolator uses Show instances: ${Money(5)} and ${Option(3)}")

  section("Order: total ordering with min/max/compare")
  println(Money(3) max Money(7))
  println(List(Money(5), Money(1)).sorted(using Order[Money].toOrdering))
  println(1 comparison 2)                  // Comparison.LessThan

  section("Semigroup & Monoid: combining")
  println(1 |+| 2)
  println("ab" |+| "cd")
  println(List(1) |+| List(2, 3))
  println(Map("a" -> 1, "b" -> 2) |+| Map("a" -> 10))      // merges values with Int's |+|
  println(Option(1) |+| None |+| Option(5))
  println(List(Stats(1, 10, Set("x")), Stats(2, 5, Set("y"))).combineAll)   // derived Monoid
  println(Monoid[String].empty.isEmpty)
  println(List("a", "b", "c").foldMap(_.toUpperCase))
  println((1, "x") |+| (2, "y"))                            // tuples combine component-wise

  section("Functor: map over any F[_]")
  println(Functor[List].map(List(1, 2))(_ + 1))
  println(Option(1).fmap(_ * 10))                           // fmap: map via the Functor instance
  println(List(Option(1), None).nested.map(_ + 1).value)    // map through two layers
  println(Functor[List].lift((_: Int) * 2)(List(1, 2)))
  println(List(1, 2).as("x"))
  println(Option(5).void)
  println(List(1, 2).fproduct(_ * 100))

  section("Applicative: combine INDEPENDENT effects")
  println((Option(1), Option(2), Option(3)).mapN(_ + _ + _))
  println((Option(1), None: Option[Int]).mapN(_ + _))
  println((List(1, 2), List("a", "b")).tupled)              // cartesian product
  println(1.pure[Option])
  println(Option(1) *> Option("right"))                     // keep the right result
  println(Option(1) <* Option("ignored"))

  section("Monad: SEQUENTIAL dependent effects")
  println(Option(3).flatMap(x => if x > 0 then Some(x * 2) else None))
  println(Monad[Option].ifM(Option(true))(Option("yes"), Option("no")))
  println(Option(5).flatTap(x => Option(println(s"  saw $x"))))
  // tailRecM: stack-safe monadic loops
  println(Monad[Option].tailRecM(0)(n => if n < 100_000 then Some(Left(n + 1)) else Some(Right(n))))

  section("Traverse: turn F[G[A]] inside out")
  def parse(s: String): Option[Int] = s.toIntOption
  println(List("1", "2", "3").traverse(parse))              // Some(List(1, 2, 3))
  println(List("1", "x", "3").traverse(parse))              // None
  println(List(Option(1), Option(2)).sequence)
  println(List(Right(1), Left("e1"), Left("e2")).sequence)  // first Left (Either is fail-fast)
  println(List(1, 2, 3).traverse(n => if n > 0 then Right(n) else Left(s"bad $n")))
  println(Vector(1, 2).flatTraverse(n => Option(Vector(n, n))))

  section("Foldable")
  println(List(1, 2, 3).foldMap(_.toString))
  println(Foldable[List].combineAll(List(1, 2, 3)))
  println(List(1, 2, 3).foldM(0)((acc, n) => if n < 3 then Option(acc + n) else None))
  println(Option(5).existsM(x => List(x > 1)))

  section("Either syntax")
  val e: Either[String, Int] = 5.asRight[String]
  println(e)
  println("boom".asLeft[Int].leftMap(_.toUpperCase))
  println(e.ensure("too small")(_ > 10))
  println(Either.catchNonFatal("x".toInt).leftMap(_.getClass.getSimpleName))
  println(Option(3).toRight("missing"))

def section(title: String): Unit = println(s"\n=== $title ===")
