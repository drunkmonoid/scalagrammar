//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/07-contextual/02_type_classes.scala

package solutions.contextual.showtree

case class Tree[A](value: A, children: List[Tree[A]] = Nil)

trait Show[T]:
  def show(t: T): String

object Show:
  def apply[T](using s: Show[T]): Show[T] = s
  given Show[String] = identity(_)
  given Show[Int] = _.toString

  given [A](using sa: Show[A]): Show[Tree[A]] with
    def show(t: Tree[A]): String =
      def format(tree: Tree[A], indent: Int): List[String] =
        val prefix = " " * indent
        val line = s"$prefix${sa.show(tree.value)}"
        line :: tree.children.flatMap(c => format(c, indent + 2))
      format(t, 0).mkString("\n")

class ShowTreeSuite extends munit.FunSuite:
  test("shows single leaf node") {
    val leaf = Tree(42)
    assertEquals(Show[Tree[Int]].show(leaf), "42")
  }

  test("shows tree with one level of children") {
    val tree = Tree("root", List(Tree("child1"), Tree("child2")))
    val expected =
      """root
        |  child1
        |  child2""".stripMargin
    assertEquals(Show[Tree[String]].show(tree), expected)
  }

  test("shows deeply nested tree with two-space increments") { // ⚠
    val nested = Tree(1, List(Tree(2, List(Tree(3)))))
    val expected =
      """1
        |  2
        |    3""".stripMargin
    assertEquals(Show[Tree[Int]].show(nested), expected)
  }
