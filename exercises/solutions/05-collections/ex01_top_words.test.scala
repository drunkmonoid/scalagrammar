//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/05-collections/04_operations.scala

package solutions.collections.topwords

def topWords(words: List[String], n: Int): List[(String, Int)] =
  if n <= 0 then Nil
  else
    words
      .groupMapReduce(identity)(_ => 1)(_ + _)
      .toList
      .sortBy { case (word, count) => (-count, word) }
      .take(n)

class TopWordsSuite extends munit.FunSuite:
  test("finds top words with distinct frequencies") {
    val input = List("scala", "java", "scala", "kotlin", "scala", "java")
    assertEquals(topWords(input, 2), List(("scala", 3), ("java", 2)))
  }

  test("breaks ties in frequency alphabetically") { // ⚠
    val input = List("banana", "apple", "banana", "apple", "cherry")
    assertEquals(topWords(input, 2), List(("apple", 2), ("banana", 2)))
  }

  test("handles empty inputs or n <= 0") {
    assertEquals(topWords(Nil, 5), Nil)
    assertEquals(topWords(List("a", "b"), 0), Nil)
    assertEquals(topWords(List("a", "b"), -1), Nil)
  }
