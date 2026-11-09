//> using scala 3.9.0
//> using jvm 21
//> using dep com.softwaremill.ox::core:1.1.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: ecosystem/direct-style/ox.scala

package exercises.ziodirect.oxpartimeout

import ox.*
import scala.concurrent.duration.*

// Task:
// Implement `fetchBoth[A, B](callA: () => A, callB: () => B, timeout: FiniteDuration): Option[(A, B)]`.
// Using Ox structured concurrency:
// 1. Run `callA()` and `callB()` in parallel with `par`.
// 2. Wrap the execution in `timeoutOption(timeout)`.
// If both finish within the timeout, return `Some((a, b))`.
// If the timeout expires before both finish, return `None`.
// Example:
//   fetchBoth(() => 1, () => "two", 500.millis) == Some((1, "two"))

def fetchBoth[A, B](callA: () => A, callB: () => B, timeout: FiniteDuration): Option[(A, B)] = ???

class OxParTimeoutSuite extends munit.FunSuite:
  test("completes both calls within timeout returning Some") {
    val res = fetchBoth(
      () => { sleep(20.millis); "result-a" },
      () => { sleep(30.millis); 42 },
      500.millis
    )
    assertEquals(res, Some(("result-a", 42)))
  }

  test("returns None when one call exceeds timeout") { // ⚠
    val res = fetchBoth(
      () => { sleep(20.millis); "fast" },
      () => { sleep(300.millis); "slow" },
      100.millis
    )
    assertEquals(res, None)
  }

  test("returns fast result when timeout is generous") {
    val res = fetchBoth(
      () => 10,
      () => 20,
      200.millis
    )
    assertEquals(res, Some((10, 20)))
  }
