//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/10-metaprogramming/02_compiletime.scala

package exercises.metaprogramming.checkedport

import scala.compiletime.error

// Task:
// Implement `inline def port(inline n: Int): Int`.
// Check at compile time that `n` is a valid port (1 to 65535 inclusive).
// If `n` is within range, return `n`.
// Otherwise, raise `compiletime.error`.

inline def port(inline n: Int): Int = ???

class CheckedPortSuite extends munit.FunSuite:
  test("accepts valid port numbers") {
    assertEquals(port(80), 80)
    assertEquals(port(443), 443)
    assertEquals(port(8080), 8080)
  }

  test("accepts boundary ports 1 and 65535") {
    assertEquals(port(1), 1)
    assertEquals(port(65535), 65535)
  }

  test("rejects out of range ports at compile time") { // ⚠
    assert(compileErrors("port(0)").nonEmpty)
    assert(compileErrors("port(-1)").nonEmpty)
    assert(compileErrors("port(65536)").nonEmpty)
    assert(compileErrors("port(70000)").nonEmpty)
  }
