//> using scala 3.9.0
// Run: scala-cli run lessons/08-errors/02_option_either_try.scala
//
// Errors as values:
//   Option[A]        — absence (no reason)
//   Either[E, A]     — failure with a typed reason (right-biased since 2.12)
//   Try[A]           — Success(a) or Failure(throwable): bridges exception-throwing code

package errors.values

import scala.util.{Try, Success, Failure}
import scala.util.boundary, boundary.break

case class User(id: Int, name: String, managerId: Option[Int])
val users = Map(1 -> User(1, "Ada", None), 2 -> User(2, "Bob", Some(1)), 3 -> User(3, "Cy", Some(99)))

@main def run(): Unit =
  section("Option: constructing")
  println(Option(null))              // None — Option.apply guards against null
  println(Some(null))                // ⚠ Some(null) — Some does not
  println(Option.when(2 > 1)("yes"))
  println(Option.unless(2 > 1)("no"))
  println(users.get(9))              // Map lookups return Option

  section("Option: consuming")
  val o: Option[Int] = Some(3)
  println(o.map(_ * 2))
  println(o.flatMap(x => if x > 5 then Some(x) else None))
  println(o.filter(_ > 1))
  println(o.getOrElse(0))
  println(o.orElse(Some(9)))
  println(o.fold("empty")(x => s"value $x"))
  println(o.contains(3) -> o.exists(_ > 2) -> o.forall(_ > 100))
  println(o.toList ++ None.toList)
  println(o.zip(Some("a")))
  println(o.toRight("missing"))      // Option → Either
  o.foreach(x => println(s"  foreach saw $x"))
  // ⚠ .get throws on None — avoid it
  try None.get catch case e: NoSuchElementException => println(s"  None.get → $e")

  section("Option chaining with for")
  def managerName(id: Int): Option[String] =
    for
      u   <- users.get(id)
      mid <- u.managerId
      m   <- users.get(mid)
    yield m.name
  println((1 to 4).map(managerName))  // None, Some(Ada), None (manager 99 missing), None

  section("Either: right-biased")
  def parseAge(s: String): Either[String, Int] =
    s.toIntOption.toRight(s"'$s' is not a number").flatMap { n =>
      if n < 0 then Left(s"$n is negative") else Right(n)
    }
  println(parseAge("42"))
  println(parseAge("x"))
  println(parseAge("-1"))
  println(parseAge("42").map(_ + 1))
  println(parseAge("x").left.map(_.toUpperCase))
  println(parseAge("x").getOrElse(0))
  println(parseAge("7").fold(err => s"error: $err", age => s"age $age"))
  println(parseAge("x").swap)
  println(Either.cond(true, "ok", "bad"))
  println(parseAge("x").toOption)
  // sequence: List[Either] → Either[List] (stops at the first Left)
  def sequence[E, A](xs: List[Either[E, A]]): Either[E, List[A]] =
    xs.foldRight(Right(Nil): Either[E, List[A]]) { (e, acc) => for a <- e; as <- acc yield a :: as }
  println(sequence(List("1", "2").map(parseAge)))
  println(sequence(List("1", "x", "y").map(parseAge)))   // first error only

  section("Typed errors with an ADT")
  def findUser(id: Int): Either[AppError, User] =
    users.get(id).toRight(AppError.NotFound(id))
  def validate(u: User): Either[AppError, User] =
    if u.name.length < 3 then Left(AppError.Invalid(s"name too short: ${u.name}")) else Right(u)
  for id <- List(1, 2, 5) do
    val res = for
      u <- findUser(id)
      v <- validate(u)
    yield v.name
    println(s"  $id → $res")

  section("Try: wrap exception-throwing code")
  val t1 = Try("42".toInt)
  val t2 = Try("x".toInt)
  println(t1 -> t2)
  println(t2.getOrElse(-1))
  println(t2.recover { case _: NumberFormatException => 0 })
  println(t2.recoverWith { case _ => Try("7".toInt) })
  println(t1.map(_ * 2).filter(_ > 100))       // Failure(NoSuchElementException: Predicate does not hold)
  println(t2.toOption -> t2.toEither)
  println(t2.failed.map(_.getClass.getSimpleName))
  t2 match
    case Success(v) => println(s"  ok $v")
    case Failure(e) => println(s"  failed with ${e.getClass.getSimpleName}")
  // ⚠ Try only catches NonFatal exceptions; fatal ones still propagate.
  // ⚠ Try is eager: Try(expr) runs expr immediately.

  section("Converting between them")
  val opt: Option[Int] = Some(1)
  val eit: Either[String, Int] = opt.toRight("none")
  // Either.toTry needs the Left type to be a Throwable, so map the error first:
  val tr: Try[Int] = eit.left.map(msg => RuntimeException(msg)).toTry
  println(eit.toOption)
  println(tr)

  section("Early return without exceptions: boundary + break")
  def firstNegative(xs: List[Int]): Option[Int] =
    boundary:
      for x <- xs do if x < 0 then break(Some(x))
      None
  println(firstNegative(List(1, -2, 3)))
  // An Option-shaped "?" operator built with boundary (like Rust's ?) — see `optional` below
  import optional.?
  def sumManagers(a: Int, b: Int): Option[Int] = optional {
    val ma = users.get(a).?.managerId.?
    val mb = users.get(b).?.managerId.?
    ma + mb
  }
  println(sumManagers(2, 3))    // Some(100)
  println(sumManagers(1, 2))    // None — user 1 has no manager

  section("Which to use?")
  // Option  – a value may legitimately be missing
  // Either  – a computation can fail and the caller needs to know why (domain errors)
  // Try     – interop with throwing APIs; convert to Either at the boundary
  // Exceptions – truly exceptional, unrecoverable situations; or inside effect systems
  println("see comments")

enum AppError:
  case NotFound(id: Int)
  case Invalid(msg: String)

object optional:
  // Label is contravariant, so the Label[Option[A]] created by boundary can serve as the
  // Label[None.type] that `?` needs to break with None.
  inline def apply[A](inline body: boundary.Label[None.type] ?=> A): Option[A] =
    boundary(Some(body))
  extension [A](o: Option[A])
    inline def ?(using label: boundary.Label[None.type]): A = o match
      case Some(a) => a
      case None    => break(None)

def section(title: String): Unit = println(s"\n=== $title ===")
