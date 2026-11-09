//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/05-collections/04_operations.scala

package solutions.collections.groupruns

def groupRuns(xs: List[Int]): List[List[Int]] =
  xs.foldRight(List.empty[List[Int]]) { (x, acc) =>
    acc match
      case (h :: t) :: rest if h == x => (x :: h :: t) :: rest
      case _                          => List(x) :: acc
  }

class GroupRunsSuite extends munit.FunSuite:
  test("groups consecutive duplicate values") {
    val input = List(1, 1, 2, 3, 3, 3, 1)
    val expected = List(List(1, 1), List(2), List(3, 3, 3), List(1))
    assertEquals(groupRuns(input), expected)
  }

  test("handles empty and all-identical lists") {
    assertEquals(groupRuns(Nil), Nil)
    assertEquals(groupRuns(List(7, 7, 7)), List(List(7, 7, 7)))
  }

  test("handles list with no consecutive duplicates") { // ⚠
    assertEquals(groupRuns(List(1, 2, 3, 4)), List(List(1), List(2), List(3), List(4)))
  }
