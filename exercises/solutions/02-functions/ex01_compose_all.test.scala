//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/02-functions/02_function_values.scala

package solutions.functions.composeall

def composeAll[A](fs: List[A => A]): A => A =
  fs.foldLeft[A => A](identity)((acc, f) => acc.andThen(f))

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
