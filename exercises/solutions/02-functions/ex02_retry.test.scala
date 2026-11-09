//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/02-functions/04_by_name_laziness.scala

package solutions.functions.retry

def retry[A](times: Int)(action: => A): Either[Throwable, A] =
  val maxAttempts = math.max(1, times)
  var lastError: Throwable = null
  var attempt = 0
  var result: Option[A] = None

  while attempt < maxAttempts && result.isEmpty do
    attempt += 1
    try
      result = Some(action)
    catch
      case t: Throwable =>
        lastError = t

  result match
    case Some(value) => Right(value)
    case None        => Left(lastError)

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
