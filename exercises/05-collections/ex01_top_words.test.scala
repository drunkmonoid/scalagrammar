//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/05-collections/04_operations.scala

package exercises.collections.topwords

// Task:
// Implement `topWords(words: List[String], n: Int): List[(String, Int)]`.
// It returns the `n` most frequent words in `words` as `(word, count)` pairs in descending order of frequency.
// ⚠ Ties in frequency must be broken alphabetically in ascending order by word.
// If n <= 0, return Nil.
// Example: topWords(List("banana", "apple", "banana", "apple", "cherry"), 2) -> List(("apple", 2), ("banana", 2))

def topWords(words: List[String], n: Int): List[(String, Int)] = ???

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
