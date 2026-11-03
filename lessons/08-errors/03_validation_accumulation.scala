//> using scala 3.9.0
// Run: scala-cli run lessons/08-errors/03_validation_accumulation.scala
//
// Either short-circuits on the first error (monadic). For forms, configs, and APIs you
// usually want ALL errors (applicative). This lesson builds a tiny Validated from scratch;
// cats.data.Validated / ValidatedNec is the production version (ecosystem/typelevel).

package errors.validation

// ---------- a minimal Validated ----------
enum Validated[+E, +A]:
  case Valid(a: A)
  case Invalid(errors: List[E])

  def map[B](f: A => B): Validated[E, B] = this match
    case Valid(a)     => Valid(f(a))
    case Invalid(es)  => Invalid(es)

  // Combine two independent validations, accumulating errors from both sides
  def zip[E2 >: E, B](that: Validated[E2, B]): Validated[E2, (A, B)] = (this, that) match
    case (Valid(a), Valid(b))         => Valid((a, b))
    case (Invalid(e1), Invalid(e2))   => Invalid(e1 ++ e2)
    case (Invalid(e1), _)             => Invalid(e1)
    case (_, Invalid(e2))             => Invalid(e2)

  def toEither: Either[List[E], A] = this match
    case Valid(a)    => Right(a)
    case Invalid(es) => Left(es)

object Validated:
  def valid[A](a: A): Validated[Nothing, A] = Valid(a)
  def invalid[E](e: E): Validated[E, Nothing] = Invalid(List(e))
  def cond[E, A](ok: Boolean, a: => A, e: => E): Validated[E, A] = if ok then valid(a) else invalid(e)

import Validated.*

// ---------- domain ----------
case class Registration(username: String, email: String, age: Int)

enum FieldError:
  case Blank(field: String)
  case TooShort(field: String, min: Int)
  case BadFormat(field: String)
  case OutOfRange(field: String, value: Int)

def validateUsername(u: String): Validated[FieldError, String] =
  if u.isBlank then invalid(FieldError.Blank("username"))
  else cond(u.length >= 3, u, FieldError.TooShort("username", 3))

def validateEmail(e: String): Validated[FieldError, String] =
  cond(e.matches("""[^@\s]+@[^@\s]+"""), e, FieldError.BadFormat("email"))

def validateAge(a: Int): Validated[FieldError, Int] =
  cond(a >= 13 && a <= 130, a, FieldError.OutOfRange("age", a))

def validate(u: String, e: String, a: Int): Validated[FieldError, Registration] =
  validateUsername(u).zip(validateEmail(e)).zip(validateAge(a)).map {
    case ((user, email), age) => Registration(user, email, age)
  }

// The fail-fast version, for comparison
def validateFast(u: String, e: String, a: Int): Either[List[FieldError], Registration] =
  for
    user  <- validateUsername(u).toEither
    email <- validateEmail(e).toEither
    age   <- validateAge(a).toEither
  yield Registration(user, email, age)

@main def run(): Unit =
  section("All valid")
  println(validate("ada", "ada@x.org", 36))

  section("Accumulating: every problem reported")
  println(validate("", "not-an-email", 7))
  println(validate("al", "al@x", 200))

  section("Fail fast: only the first problem")
  println(validateFast("", "not-an-email", 7))

  section("Traversing a list, accumulating")
  def traverse[E, A, B](as: List[A])(f: A => Validated[E, B]): Validated[E, List[B]] =
    as.foldRight(valid(Nil): Validated[E, List[B]]) { (a, acc) =>
      f(a).zip(acc).map(_ :: _)
    }
  println(traverse(List(20, 5, 300, 40))(validateAge))
  println(traverse(List(20, 40))(validateAge))

  section("Rendering errors for users")
  validate("", "x", 1) match
    case Validated.Valid(r)       => println(s"  registered $r")
    case Validated.Invalid(errs)  => errs.foreach {
      case FieldError.Blank(f)         => println(s"  • $f must not be blank")
      case FieldError.TooShort(f, n)   => println(s"  • $f needs at least $n characters")
      case FieldError.BadFormat(f)     => println(s"  • $f has an invalid format")
      case FieldError.OutOfRange(f, v) => println(s"  • $f: $v is out of range")
    }

def section(title: String): Unit = println(s"\n=== $title ===")
