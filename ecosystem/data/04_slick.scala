//> using scala 3.9.0
//> using dep com.typesafe.slick::slick:3.5.2
//> using dep com.h2database:h2:2.5.252
//> using dep org.slf4j:slf4j-nop:2.0.20
// See also: ecosystem/data/01_doobie.scala
// Run: scala-cli run ecosystem/data/04_slick.scala
//
// Slick functional-relational mapping (FRM) on in-memory H2:
// 1. Table schema mapping and programmatic DDL creation
// 2. Composable query combinators: filter, map, sortBy, and joins
// 3. Inspecting generated SQL statements via .result.statements
// 4. Atomic transaction demarcation with .transactionally and rollback on failure
// 5. Clean connection pool termination

package ecosystem.data.slickdemo

import slick.jdbc.H2Profile.api.*
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

case class User(id: Long, name: String, email: String)
case class Order(id: Long, userId: Long, product: String, amount: Double)

class Users(tag: Tag) extends Table[User](tag, "users"):
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def email = column[String]("email")
  def * = (id, name, email).mapTo[User]

class Orders(tag: Tag) extends Table[Order](tag, "orders"):
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Long]("user_id")
  def product = column[String]("product")
  def amount = column[Double]("amount")
  def * = (id, userId, product, amount).mapTo[Order]

val users = TableQuery[Users]
val orders = TableQuery[Orders]

@main def run(): Unit =
  val db = Database.forURL("jdbc:h2:mem:slicktest;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  try
    section("1. Schema creation and inserts")
    val setupAction = (users.schema ++ orders.schema).create andThen (
      users ++= Seq(
        User(1L, "Alice", "alice@example.com"),
        User(2L, "Bob", "bob@example.com"),
        User(3L, "Charlie", "charlie@example.com")
      )
    ) andThen (
      orders ++= Seq(
        Order(1L, 1L, "Mechanical Keyboard", 120.0),
        Order(2L, 1L, "Ergonomic Mouse", 60.0),
        Order(3L, 2L, "4K Monitor", 400.0)
      )
    )
    Await.result(db.run(setupAction), 5.seconds)
    println("Schema created and initial records inserted.")

    section("2. Filtering, mapping, sorting and generated SQL statements")
    val query = users
      .filter(_.name =!= "Charlie")
      .sortBy(_.name.asc)
      .map(u => (u.name, u.email))

    val generatedStatements = query.result.statements
    println(s"Generated SQL:\n  ${generatedStatements.mkString("\n  ")}")

    val queryResult = Await.result(db.run(query.result), 5.seconds)
    println(s"Query result: $queryResult")
    assert(queryResult == Vector(("Alice", "alice@example.com"), ("Bob", "bob@example.com")))

    section("3. Inner join between Users and Orders")
    val joinQuery = for
      (u, o) <- users.join(orders).on(_.id === _.userId)
    yield (u.name, o.product, o.amount)

    val joinStatements = joinQuery.result.statements
    println(s"Generated Join SQL:\n  ${joinStatements.mkString("\n  ")}")

    val joinResult = Await.result(db.run(joinQuery.result), 5.seconds)
    println(s"Join result: $joinResult")
    assert(joinResult.length == 3)

    section("4. Transaction rollback with .transactionally")
    val failingTx = (for
      _ <- users += User(4L, "Dave", "dave@example.com")
      _ <- DBIO.failed(new RuntimeException("Simulated transaction rollback"))
    yield ()).transactionally

    val txFailed = try
      Await.result(db.run(failingTx), 5.seconds)
      false
    catch
      case _: RuntimeException => true

    println(s"Transaction failed as expected: $txFailed")
    assert(txFailed)

    val usersAfterRollback = Await.result(db.run(users.result), 5.seconds)
    val names = usersAfterRollback.map(_.name)
    println(s"Users present after rollback: $names")
    assert(!names.contains("Dave"))
    assert(names.contains("Alice") && names.contains("Bob") && names.contains("Charlie"))
  finally
    db.close()
    println("Database connection pool closed.")

def section(title: String): Unit = println(s"\n=== $title ===")
