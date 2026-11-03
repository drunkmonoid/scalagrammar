package demo.core

class StatsSuite extends munit.FunSuite:
  test("mean of an empty list is None") {
    assertEquals(Stats.mean(Nil), None)
  }
  test("mean") {
    assertEquals(Stats.mean(List(1.0, 2.0, 3.0)), Some(2.0))
  }
  test("word counts merge across lines") {
    assertEquals(Stats.wordCounts(List("a b", "b c")), Map("a" -> 1, "b" -> 2, "c" -> 1))
  }
