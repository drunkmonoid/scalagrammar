//> using scala 3.9.0
//> using dep org.typelevel::cats-core:2.13.0
// Run: scala-cli run ecosystem/typelevel/02_cats_data_types.scala
//
// cats data types: NonEmptyList, Validated/ValidatedNec, Chain, Ior, Kleisli, State, Writer,
// Reader, and monad transformers (OptionT, EitherT).

package ecosystem.cats.datatypes

import cats.*
import cats.data.*
import cats.syntax.all.*

case class User(name: String, email: String, age: Int)

@main def run(): Unit =
  section("NonEmptyList: a List with at least one element")
  val nel = NonEmptyList.of(3, 1, 2)
  println(nel.head)                          // safe: no Option
  println(nel.toList.sorted)
  println(nel.reduce)                        // reduce needs no empty value: 6
  println(NonEmptyList.fromList(List.empty[Int]))
  println(List(1, 2, 3).groupByNel(_ % 2))   // Map[Int, NonEmptyList[Int]]

  section("Validated: accumulate errors (applicative)")
  def name(n: String): ValidatedNec[String, String] =
    if n.nonEmpty then n.validNec else "name is empty".invalidNec
  def email(e: String): ValidatedNec[String, String] =
    if e.contains("@") then e.validNec else s"bad email '$e'".invalidNec
  def age(a: Int): ValidatedNec[String, Int] =
    if a >= 18 then a.validNec else s"too young: $a".invalidNec
  println((name("Ada"), email("ada@x.org"), age(36)).mapN(User.apply))
  println((name(""), email("nope"), age(7)).mapN(User.apply))      // all three errors
  // Switch to Either for fail-fast steps, and back:
  println(age(7).toEither)
  println(Either.left[String, Int]("e").toValidatedNec)
  // A list of validations at once:
  println(List(20, 5, 40).traverse(age))

  section("Chain: O(1) append and prepend (used inside ValidatedNec)")
  val c = Chain(1, 2) ++ Chain.one(3) :+ 4
  println(c.toList)
  println(c.prepend(0).length)

  section("Ior: Left, Right or Both (warnings + result)")
  def parse(s: String): Ior[NonEmptyChain[String], Int] =
    s.trim.toIntOption match
      case Some(n) if s != s.trim => Ior.both(NonEmptyChain(s"trimmed '$s'"), n)
      case Some(n)                => Ior.right(n)
      case None                   => Ior.leftNec(s"not a number '$s'")
  println(parse("42"))
  println(parse(" 42 "))
  println(parse("x"))
  println((parse(" 1"), parse("2 ")).mapN(_ + _))   // warnings accumulate, result computed

  section("Kleisli: composing A => F[B] functions")
  val parseK: Kleisli[Option, String, Int] = Kleisli(_.toIntOption)
  val recipK: Kleisli[Option, Int, Double] = Kleisli(n => Option.when(n != 0)(1.0 / n))
  val both = parseK.andThen(recipK)
  println(both.run("4") -> both.run("0") -> both.run("x"))

  section("Reader: dependency injection as a Kleisli[Id, Env, A]")
  case class Config(greeting: String, punctuation: String)
  val greet: Reader[Config, String => String] = Reader(cfg => n => s"${cfg.greeting}, $n${cfg.punctuation}")
  val shout: Reader[Config, String] = greet.map(f => f("ada").toUpperCase)
  println(shout.run(Config("hello", "!")))

  section("State: pure state threading")
  type Stack = List[Int]
  def push(x: Int): State[Stack, Unit] = State.modify(x :: _)
  val pop: State[Stack, Option[Int]] = State { s => (s.drop(1), s.headOption) }
  val program = for
    _ <- push(1)
    _ <- push(2)
    _ <- push(3)
    a <- pop
    b <- pop
  yield (a, b)
  println(program.run(Nil).value)          // (final state, result)
  // A random number generator as State
  val nextLong: State[Long, Long] = State(seed => {
    val n = seed * 6364136223846793005L + 1442695040888963407L
    (n, n >>> 33)
  })
  println(nextLong.replicateA(3).runA(42L).value)

  section("Writer: values with an accumulated log")
  def step(n: Int): Writer[Vector[String], Int] = Writer(Vector(s"got $n"), n * 2)
  val w = for
    a <- step(1)
    b <- step(a)
    _ <- Writer.tell(Vector("done"))
  yield b
  println(w.run)

  section("Monad transformers: OptionT / EitherT")
  // Without transformers, Future[Option[A]] needs nested maps. EitherT[F, E, A] wraps
  // F[Either[E, A]] and gives a single flatMap over both layers.
  def findUser(id: Int): Eval[Option[String]] = Eval.now(Option.when(id == 1)("ada"))
  def findEmail(n: String): Eval[Option[String]] = Eval.now(Some(s"$n@x.org"))
  val lookup = for
    n <- OptionT(findUser(1))
    e <- OptionT(findEmail(n))
  yield e
  println(lookup.value.value)
  def validateE(n: Int): EitherT[Eval, String, Int] =
    EitherT.cond[Eval](n > 0, n, s"$n is not positive")
  println((for a <- validateE(1); b <- validateE(-2) yield a + b).value.value)

  section("Eval: control evaluation (now / later / always) & stack safety")
  val later = Eval.later { println("  computed once"); 42 }
  println(later.value); println(later.value)
  def evenOdd(n: Int): Eval[Boolean] = if n == 0 then Eval.True else Eval.defer(evenOdd(n - 1).map(!_))
  println(evenOdd(1_000_000).value)        // stack-safe

def section(title: String): Unit = println(s"\n=== $title ===")
