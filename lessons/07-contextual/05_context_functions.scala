//> using scala 3.9.0
// Run: scala-cli run lessons/07-contextual/05_context_functions.scala
//
// Context function types:  A ?=> B   — a function whose parameter is a CONTEXT parameter.
// Used for DSLs, "implicit environments" (transactions, builders, capabilities).

package contextual.ctxfunctions

import scala.collection.mutable.ListBuffer

// ---------- a simple environment ----------
case class Transaction(id: Int):
  private val log = ListBuffer.empty[String]
  def record(op: String): Unit = log += op
  def ops: List[String] = log.toList

type Transactional[A] = Transaction ?=> A        // "a computation that needs a Transaction"

def insert(row: String): Transactional[Unit] =
  summon[Transaction].record(s"INSERT $row")

def update(row: String): Transactional[Unit] =
  summon[Transaction].record(s"UPDATE $row")

def transaction[A](body: Transactional[A]): (A, List[String]) =
  given tx: Transaction = Transaction(1)         // provide the context
  val result = body                              // body is applied to tx automatically
  (result, tx.ops)

// ---------- a builder DSL (like HTML/table builders) ----------
class Table:
  val rows = ListBuffer.empty[Row]
  override def toString = rows.mkString("Table(\n", "\n", "\n)")
class Row:
  val cells = ListBuffer.empty[String]
  override def toString = cells.mkString("  Row(", " | ", ")")

def table(init: Table ?=> Unit): Table =
  given t: Table = Table()
  init
  t
def row(init: Row ?=> Unit)(using t: Table): Unit =
  given r: Row = Row()
  init
  t.rows += r
def cell(s: String)(using r: Row): Unit = r.cells += s

// ---------- capabilities: "can only call this inside a context" ----------
class CanLog private ():
  def log(s: String): Unit = println(s"  [log] $s")
object CanLog:
  def run[A](body: CanLog ?=> A): A = body(using new CanLog())

def businessLogic(x: Int)(using CanLog): Int =
  summon[CanLog].log(s"computing with $x")
  x * 2

@main def run(): Unit =
  section("Implicit environment")
  val (res, ops) = transaction {
    insert("a")            // each call receives the Transaction automatically
    update("b")
    42
  }
  println(s"result=$res ops=$ops")

  section("Builder DSL")
  val t = table {
    row { cell("name"); cell("age") }
    row { cell("Ada");  cell("36") }
  }
  println(t)

  section("Capabilities")
  val r = CanLog.run { businessLogic(21) }
  println(r)
  // businessLogic(1) outside CanLog.run → "No given instance of type CanLog was found"

  section("Context function values")
  val greet: String ?=> String = s"Hello, ${summon[String]}"
  locally {
    given String = "Ada"
    println(greet)                               // applied with the given String
  }
  println(greet(using "Bob"))                     // or explicitly

  section("How the compiler expands them")
  // def f: A ?=> B = body       ⇒  def f: A ?=> B = (a: A) ?=> body   (lambda with using param)
  // f                            ⇒  f(using summon[A])                when expected type is B
  // A context-function argument `{ ... }` is wrapped in `(x: A) ?=> { ... }` automatically,
  // which is why `table { row { ... } }` can see the givens.
  println("see comments")

def section(title: String): Unit = println(s"\n=== $title ===")
