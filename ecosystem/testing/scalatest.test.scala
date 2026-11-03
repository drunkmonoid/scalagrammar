//> using scala 3.9.0
//> using test.dep org.scalatest::scalatest:3.2.19
// Run: scala-cli test ecosystem/testing/scalatest.test.scala
//
// ScalaTest: the most widely used Scala testing library. It offers many "styles"
// (AnyFunSuite, AnyFlatSpec, AnyWordSpec, AnyFreeSpec, ...) and rich matchers.

package ecosystem.testing.scalatestdemo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.OptionValues.*
import org.scalatest.EitherValues.*
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// ---------- code under test ----------
final case class Account(balance: BigDecimal):
  def withdraw(amount: BigDecimal): Either[String, Account] =
    if amount <= 0 then Left("amount must be positive")
    else if amount > balance then Left("insufficient funds")
    else Right(copy(balance = balance - amount))

// ---------- FunSuite style ----------
class AccountFunSuite extends AnyFunSuite:
  test("withdrawing reduces the balance") {
    assert(Account(100).withdraw(30) == Right(Account(70)))
  }
  test("cannot overdraw") {
    assert(Account(10).withdraw(30).isLeft)
  }

// ---------- FlatSpec + Matchers ----------
class AccountSpec extends AnyFlatSpec with Matchers:
  "An account" should "allow withdrawals within the balance" in {
    Account(100).withdraw(100).value.balance shouldBe BigDecimal(0)
  }
  it should "reject negative amounts" in {
    Account(100).withdraw(-1).left.value should include("positive")
  }
  "Matchers" should "cover collections, strings, options, exceptions" in {
    List(1, 2, 3) should contain allOf (1, 3)
    List(1, 2, 3) should have size 3
    "scala 3" should startWith("scala")
    Some(5).value shouldBe 5
    an[ArithmeticException] should be thrownBy (1 / 0)
    3.14 shouldBe 3.1 +- 0.1
    Map("a" -> 1) should contain key "a"
  }

// ---------- WordSpec ----------
class AccountWordSpec extends AnyWordSpec with Matchers:
  "Account.withdraw" when {
    "the amount is too large" should {
      "return an error" in {
        Account(1).withdraw(2) shouldBe Left("insufficient funds")
      }
    }
  }

// ---------- async ----------
class FutureSpec extends AnyFlatSpec with Matchers with ScalaFutures:
  "a Future" should "eventually succeed" in {
    whenReady(Future(21 * 2)) { _ shouldBe 42 }
  }
  it should "also work with futureValue" in {
    Future("ok").futureValue shouldBe "ok"
  }

// ---------- tags & pending ----------
class MiscSuite extends AnyFunSuite:
  test("pending test") { pending }
  ignore("ignored test") { fail("never runs") }
  test("assertThrows / intercept") {
    val e = intercept[IllegalArgumentException](require(false, "boom"))
    assert(e.getMessage.contains("boom"))
  }
