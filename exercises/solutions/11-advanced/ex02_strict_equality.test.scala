//> using scala 3.9.0
//> using options -language:strictEquality
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/11-advanced/02_multiversal_equality.scala

package solutions.advanced.strictequality

// Task:
// Under `-language:strictEquality`, types cannot be compared with `==` unless a
// `CanEqual` instance is available.
// In this exercise, `Person(id: Long, name: String)` does not yet derive `CanEqual`.
// Add `derives CanEqual` to `case class Person(id: Long, name: String)` so that
// comparisons between `Person` instances compile cleanly.
// Implement `makePerson` to return a `Person` instance.

case class Person(id: Long, name: String) derives CanEqual

def makePerson(id: Long, name: String): Person = Person(id, name)

class StrictEqualitySuite extends munit.FunSuite:
  test("Person equality compiles under strict equality") {
    assertNoDiff(compileErrors("Person(1L, \"Alice\") == Person(2L, \"Bob\")"), "")
  }

  test("makePerson creates person instances with equal values") {
    val p1 = makePerson(42L, "Alice")
    val p2 = makePerson(42L, "Alice")
    assertEquals(p1.id, p2.id)
    assertEquals(p1.name, p2.name)
  }

  test("disallows comparing Person with primitive or unrelated type at compile time") { // ⚠
    assert(compileErrors("Person(1L, \"Alice\") == 1L").nonEmpty)
    assert(compileErrors("Person(1L, \"Alice\") == \"Alice\"").nonEmpty)
  }
