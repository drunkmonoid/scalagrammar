//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/02-functions/04_by_name_laziness.scala

package exercises.functions.retry

// Task:
// Implement `retry[A](times: Int)(action: => A): Either[Throwable, A]`.
// It attempts to evaluate `action` up to `times` times (minimum 1 attempt).
// If `action` throws an exception, it retries until it succeeds or exhausts `times` attempts.
// If all attempts fail, return `Left` with the last caught Throwable.
// If it succeeds, return `Right` with the value without evaluating `action` again.
// Example: retry(3) { 42 } -> Right(42)

def retry[A](times: Int)(action: => A): Either[Throwable, A] = ???

class RetrySuite extends munit.FunSuite:
  test("succeeds on first attempt without retrying") {
    var count = 0
    val res = retry(3) {
      count += 1
      100
    }
    assertEquals(res, Right(100))
    assertEquals(count, 1)
  }

  test("retries and succeeds on subsequent attempt") {
    var count = 0
    val res = retry(3) {
      count += 1
      if count < 2 then throw new RuntimeException("transient error")
      "ok"
    }
    assertEquals(res, Right("ok"))
    assertEquals(count, 2)
  }

  test("exhausts all attempts and returns last error") { // ⚠
    var count = 0
    val res = retry(3) {
      count += 1
      throw new IllegalArgumentException(s"failure $count")
    }
    assertEquals(count, 3)
    assert(res.isLeft)
    assertEquals(res.left.toOption.map(_.getMessage), Some("failure 3"))
  }
