//> using scala 3.9.0
//> using dep org.typelevel::cats-core:2.13.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: ecosystem/typelevel/02_cats_data_types.scala

package solutions.typelevel.validatedform

import cats.data.ValidatedNec
import cats.syntax.all.*

// Task:
// Validate a three-field user registration form into `ValidatedNec[String, User]`.
// Validation rules:
// - username must not be empty or blank; otherwise "Username cannot be empty"
// - email must contain '@'; otherwise "Invalid email format"
// - age must be >= 18; otherwise "Must be at least 18 years old"
// Use cats `mapN` to combine the three validations accumulating all errors.
// Example:
//   validateForm("alice", "a@b.com", 20) == Valid(User("alice", "a@b.com", 20))
//   validateForm("", "bad", 16) == Invalid(Chain("Username cannot be empty", "Invalid email format", "Must be at least 18 years old"))

case class User(username: String, email: String, age: Int)

def validateForm(username: String, email: String, age: Int): ValidatedNec[String, User] =
  val validName =
    if username.trim.nonEmpty then username.validNec
    else "Username cannot be empty".invalidNec

  val validEmail =
    if email.contains("@") then email.validNec
    else "Invalid email format".invalidNec

  val validAge =
    if age >= 18 then age.validNec
    else "Must be at least 18 years old".invalidNec

  (validName, validEmail, validAge).mapN(User.apply)

class ValidatedFormSuite extends munit.FunSuite:
  test("accepts valid form inputs") {
    val res = validateForm("alice", "alice@example.com", 25)
    assertEquals(res.isValid, true)
    assertEquals(res.toOption, Some(User("alice", "alice@example.com", 25)))
  }

  test("rejects single invalid field with specific error") {
    val badEmail = validateForm("bob", "bob-no-at", 30)
    assertEquals(badEmail.isValid, false)
    assertEquals(badEmail.leftMap(_.toList), cats.data.Validated.invalid(List("Invalid email format")).leftMap(_.toList))
  }

  test("accumulates multiple errors across all bad fields") { // ⚠
    val allBad = validateForm("   ", "no-at", 15)
    assertEquals(allBad.isValid, false)
    val errors = allBad.fold(_.toList, _ => Nil)
    assertEquals(
      errors,
      List(
        "Username cannot be empty",
        "Invalid email format",
        "Must be at least 18 years old"
      )
    )
  }
