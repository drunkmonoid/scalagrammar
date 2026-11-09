//> using scala 3.9.0
//> using dep org.tpolecat::skunk-core:0.6.5
//> using dep io.zonky.test:embedded-postgres:2.1.0
//> using dep org.slf4j:slf4j-nop:2.0.20
// See also: ecosystem/data/01_doobie.scala
// Run: scala-cli run ecosystem/data/05_skunk.scala
//
// Pure functional PostgreSQL client with Skunk on embedded PostgreSQL:
// 1. In-process embedded Postgres lifecycle management via Resource
// 2. Strongly-typed queries and commands with composable codecs
// 3. Prepared statement lifecycle and batch execution
// 4. Transactional guarantees with automatic rollback on error

package ecosystem.data.skunkdemo

import cats.effect.{IO, IOApp, Resource}
import cats.syntax.all.*
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*
import natchez.Trace.Implicits.noop

object SkunkApp extends IOApp.Simple:

  // Resource that manages the embedded PostgreSQL server process
  val embeddedPostgres: Resource[IO, EmbeddedPostgres] =
    Resource.make(
      IO.blocking(EmbeddedPostgres.builder().start())
    )(pg =>
      IO.blocking {
        println("Stopping embedded PostgreSQL instance...")
        pg.close()
        println("PostgreSQL stopped cleanly.")
      }
    )

  // Skunk session connected to the local embedded PostgreSQL
  def session(pg: EmbeddedPostgres): Resource[IO, Session[IO]] =
    Session.single[IO](
      host = "localhost",
      port = pg.getPort,
      user = "postgres",
      database = "postgres",
      password = None
    )

  // SQL Commands & Queries with Skunk codecs
  val createTableCmd: Command[Void] =
    sql"""
      CREATE TABLE developers (
        id INT PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        language VARCHAR(50) NOT NULL
      )
    """.command

  val insertDevCmd: Command[(Int, String, String)] =
    sql"""
      INSERT INTO developers (id, name, language)
      VALUES ($int4, ${varchar(100)}, ${varchar(50)})
    """.command

  val selectDevsQuery: Query[String, (Int, String)] =
    sql"""
      SELECT id, name
      FROM developers
      WHERE language = ${varchar(50)}
      ORDER BY id
    """.query(int4 ~ varchar(100))

  val countAllQuery: Query[Void, Long] =
    sql"SELECT count(*) FROM developers".query(int8)

  def run: IO[Unit] =
    embeddedPostgres.use { pg =>
      session(pg).use { s =>
        for
          _ <- IO.println(s"Connected to embedded PostgreSQL on port ${pg.getPort}")

          _ <- section("1. Schema initialization with Command")
          _ <- s.execute(createTableCmd)
          _ <- IO.println("Table 'developers' created.")

          _ <- section("2. Prepared statements with Command and Codecs")
          _ <- s.prepare(insertDevCmd).use { ps =>
            for
              _ <- ps.execute((1, "Martin Odersky", "Scala"))
              _ <- ps.execute((2, "James Gosling", "Java"))
              _ <- ps.execute((3, "Guido van Rossum", "Python"))
            yield ()
          }
          _ <- IO.println("Inserted 3 developer records via prepared statement.")

          _ <- section("3. Parameterized Query execution")
          scalaDevs <- s.prepare(selectDevsQuery).use { ps =>
            ps.stream("Scala", 10).compile.toList
          }
          _ <- IO.println(s"Query result for language = 'Scala': $scalaDevs")
          _ <- IO(assert(scalaDevs == List((1, "Martin Odersky"))))

          _ <- section("4. Transaction rollback on error")
          _ <- s.transaction.use { _ =>
            for
              _ <- s.execute(insertDevCmd)((4, "Temporary Dev", "Scala"))
              _ <- IO.raiseError(new RuntimeException("Simulated error to trigger rollback"))
            yield ()
          }.handleErrorWith(e => IO.println(s"Transaction aborted with error: ${e.getMessage}"))

          totalCount <- s.unique(countAllQuery)
          _ <- IO.println(s"Total records after rollback: $totalCount")
          _ <- IO(assert(totalCount == 3L, s"Expected 3 records after rollback but found $totalCount"))
          _ <- IO.println("Skunk PostgreSQL demonstrations completed successfully.")
        yield ()
      }
    }

  def section(title: String): IO[Unit] =
    IO.println(s"\n=== $title ===")
