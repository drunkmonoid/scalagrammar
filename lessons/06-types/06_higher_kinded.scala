//> using scala 3.9.0
// Run: scala-cli run lessons/06-types/06_higher_kinded.scala
//
// Higher-kinded types (HKT): abstracting over type CONSTRUCTORS like List, Option, Future.
//
//   Kinds:   Int, String           : *            (proper types)
//            List, Option          : * -> *       (take one type)
//            Map, Either           : * -> * -> *
//            Functor (below)       : (* -> *) -> *
//
// Syntax: F[_] in a type-parameter list declares a type constructor parameter.

package types.hkt

import scala.compiletime.testing.typeCheckErrors

// A Functor abstracts "things you can map over"
trait Functor[F[_]]:
  extension [A](fa: F[A]) def fmap[B](f: A => B): F[B]

given Functor[List] with
  extension [A](fa: List[A]) def fmap[B](f: A => B) = fa.map(f)

// (No separate Functor[Option]: the Monad[Option] below IS a Functor[Option]. Defining both
//  would make `summon[Functor[Option]]` depend on given-prioritisation rules.)

// A custom container
case class Box[A](value: A)
given Functor[Box] with
  extension [A](fa: Box[A]) def fmap[B](f: A => B) = Box(f(fa.value))

// Code written ONCE for every Functor
def double[F[_]: Functor](fa: F[Int]): F[Int] = fa.fmap(_ * 2)
def describe[F[_]: Functor, A](fa: F[A]): F[String] = fa.fmap(a => s"<$a>")

// Monad builds on it
trait Monad[F[_]] extends Functor[F]:
  def pure[A](a: A): F[A]
  extension [A](fa: F[A])
    def flatMap[B](f: A => F[B]): F[B]
    def fmap[B](f: A => B): F[B] = fa.flatMap(a => pure(f(a)))

given Monad[Option] with
  def pure[A](a: A) = Some(a)
  extension [A](fa: Option[A]) def flatMap[B](f: A => Option[B]) = fa match
    case Some(a) => f(a)
    case None    => None

def sumAll[F[_]: Monad](a: F[Int], b: F[Int]): F[Int] =
  a.flatMap(x => b.fmap(y => x + y))

// A container parameterised by its effect: "tagless final" in one line
trait UserRepo[F[_]]:
  def find(id: Int): F[Option[String]]
object IdRepo extends UserRepo[[A] =>> A]:          // identity type lambda (see 07)
  def find(id: Int) = if id == 1 then Some("ada") else None
object ListRepo extends UserRepo[List]:
  def find(id: Int) = List(Some("ada"), None)

@main def run(): Unit =
  section("One function, many containers")
  println(double(List(1, 2, 3)))
  println(double(Option(21)))
  println(double(Box(5)))
  println(describe(List(1, 2)))

  section("Kind mismatches are compile errors")
  doesNotCompile("types.hkt.double(42)")               // Int isn't F[Int] for any functor
  doesNotCompile("summon[types.hkt.Functor[Int]]")     // Int has kind *, not * -> *
  doesNotCompile("summon[types.hkt.Functor[Map]]")     // Map has kind * -> * -> *

  section("Partially applying a 2-arg type constructor")
  // Either has kind * -> * -> *. To get a Functor we fix the left side with a type lambda:
  given [L]: Functor[[R] =>> Either[L, R]] with
    extension [A](fa: Either[L, A]) def fmap[B](f: A => B) = fa.map(f)
  println(double(Right(10): Either[String, Int]))
  println(double(Left("err"): Either[String, Int]))
  // Scala 2 needed the kind-projector plugin: Functor[Either[L, *]]. Scala 3 has native
  // type lambdas, and under -Xkind-projector also accepts `Either[L, *]`.

  section("Monad-generic code")
  println(sumAll(Option(1), Option(2)))
  println(sumAll(Option(1), Option.empty[Int]))

  section("Effect-polymorphic interfaces")
  println(IdRepo.find(1))
  println(ListRepo.find(1))

  section("Type constructors as type members")
  trait Container:
    type F[_]
    def wrap[A](a: A): F[A]
  object ListContainer extends Container:
    type F[X] = List[X]
    def wrap[A](a: A) = List(a)
  println(ListContainer.wrap(1))

  section("Higher-kinded bounds and AnyKind")
  // F[_] <: Iterable[?] — F must produce Iterables
  def firstOf[F[X] <: Iterable[X], A](fa: F[A]): Option[A] = fa.headOption
  println(firstOf(Vector(9, 8)))
  // AnyKind is a supertype of types of ALL kinds (used by metaprogramming APIs)
  def kindName[T <: AnyKind]: String = "a type of any kind"
  println(kindName[List] + " / " + kindName[Int] + " / " + kindName[Map])

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
