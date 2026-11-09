//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/02-functions/02_function_values.scala

package exercises.functions.composeall

// Task:
// Implement `composeAll[A](fs: List[A => A]): A => A`.
// Functions must be applied left to right: fs = List(f, g) produces x => g(f(x)).
// An empty list must return the identity function (x => x).
// Example: composeAll(List((x: Int) => x + 1, (x: Int) => x * 2))(3) -> (3 + 1) * 2 = 8

def composeAll[A](fs: List[A => A]): A => A = ???

class ComposeAllSuite extends munit.FunSuite:
  test("composes multiple functions left to right") {
    val fs: List[Int => Int] = List(_ + 1, _ * 2, _ - 3)
    assertEquals(composeAll(fs)(5), 9)
  }

  test("empty list returns identity") { // ⚠
    val idInt = composeAll[Int](Nil)
    assertEquals(idInt(42), 42)
    val idStr = composeAll[String](Nil)
    assertEquals(idStr("hello"), "hello")
  }

  test("single function applies as is") {
    val single: List[String => String] = List(_.toUpperCase)
    assertEquals(composeAll(single)("scala"), "SCALA")
  }
