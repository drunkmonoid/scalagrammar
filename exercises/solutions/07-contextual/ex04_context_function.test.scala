//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/07-contextual/05_context_functions.scala

package solutions.contextual.contextfunction

case class Transaction(id: String)

type Transactional[A] = Transaction ?=> A

def query(sql: String): Transactional[List[String]] =
  val tx = summon[Transaction]
  List(s"tx:${tx.id}", sql)

def runTransaction[A](id: String)(block: Transactional[A]): A =
  given Transaction = Transaction(id)
  block

class ContextFunctionSuite extends munit.FunSuite:
  test("query runs inside runTransaction context") {
    val result = runTransaction("tx-999") {
      query("SELECT * FROM users")
    }
    assertEquals(result, List("tx:tx-999", "SELECT * FROM users"))
  }

  test("multiple queries share the ambient transaction") {
    val results = runTransaction("tx-abc") {
      val q1 = query("INSERT INTO log")
      val q2 = query("COMMIT")
      q1 ++ q2
    }
    assertEquals(results, List("tx:tx-abc", "INSERT INTO log", "tx:tx-abc", "COMMIT"))
  }

  test("query outside transaction fails to compile") { // ⚠
    assert(compileErrors("query(\"SELECT 1\")").nonEmpty)
  }
