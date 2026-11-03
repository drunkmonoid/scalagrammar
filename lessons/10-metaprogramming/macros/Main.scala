// Run the whole directory:  scala-cli run --server=false lessons/10-metaprogramming/macros/

package meta.macros

import scala.compiletime.testing.typeCheckErrors

// User and Plain live in Model.scala

@main def run(): Unit =
  section("debug: source text + value")
  val a = 3
  val b = 4
  debug(a * b + 1)
  debug(List(a, b).map(_ * 2))

  section("compile-time validated literals")
  println(port(8080))
  println(typeCheckErrors("meta.macros.port(70000)").map(_.message))
  println(typeCheckErrors("val p = 80; meta.macros.port(p)").map(_.message.linesIterator.next()))

  section("generated code")
  println(pow(2.0, 8))
  println(s"  pow(x, 3) expands to: ${powCode(2.0, 3)}")

  section("quote pattern matching")
  println(simplify(a * 1 + 0))
  println(s"  simplified: ${simplifyCode(a * 1 + (b + 0))}")
  println(s"  simplified: ${simplifyCode(a * 0)}")

  section("types")
  println(typeName[Map[String, List[Int]]])
  println(isCaseClass[User] -> isCaseClass[Plain])

  section("TASTy reflection")
  println(fieldNames[User])
  println(toMap(User("Ada", 36, "ada@x.org")))

  section("positions")
  println(s"  called at $here")

  section("raw trees")
  println(s"  ${treeOf(a + 1)}")

def section(title: String): Unit = println(s"\n=== $title ===")
