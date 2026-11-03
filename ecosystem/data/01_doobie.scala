//> using scala 3.9.0
//> using dep org.tpolecat::doobie-core:1.0.0-RC12
//> using dep org.tpolecat::doobie-h2:1.0.0-RC12
//> using dep com.h2database:h2:2.5.252
// Run: scala-cli run ecosystem/data/01_doobie.scala
//
// doobie: pure functional JDBC on cats-effect. SQL is written as SQL, but:
//   * sql"... $param ..." interpolation creates PREPARED statements (no SQL injection)
//   * results are mapped to Scala types via Read/Write/Get/Put type classes (derived for case classes)
//   * ConnectionIO[A] is a program that needs a connection; a Transactor runs it in a transaction
// This lesson uses an in-memory H2 database, so it needs no setup.

package ecosystem.data.doobiedemo

import cats.effect.*
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*

case class Country(code: String, name: String, population: Long)
case class City(id: Int, name: String, countryCode: String, population: Int)

object Main extends IOApp.Simple:

  // A Transactor knows how to get connections (here: one in-memory DB kept alive while the JVM runs)
  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None,
  )

  val createSchema: ConnectionIO[Unit] =
    (sql"""CREATE TABLE country (code CHAR(3) PRIMARY KEY, name VARCHAR NOT NULL, population BIGINT)""".update.run *>
     sql"""CREATE TABLE city (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR NOT NULL,
                              country_code CHAR(3) REFERENCES country(code), population INT)""".update.run).void

  def insertCountry(c: Country): ConnectionIO[Int] =
    sql"INSERT INTO country (code, name, population) VALUES (${c.code}, ${c.name}, ${c.population})".update.run

  // Batch insert with a reusable Update
  def insertCities(cities: List[(String, String, Int)]): ConnectionIO[Int] =
    Update[(String, String, Int)]("INSERT INTO city (name, country_code, population) VALUES (?, ?, ?)")
      .updateMany(cities)

  // Queries: .query[A] maps each row to A
  def citiesIn(code: String): ConnectionIO[List[City]] =
    sql"SELECT id, name, country_code, population FROM city WHERE country_code = $code ORDER BY population DESC"
      .query[City].to[List]

  def findCountry(code: String): ConnectionIO[Option[Country]] =
    sql"SELECT code, name, population FROM country WHERE code = $code".query[Country].option

  // Fragments compose: build dynamic WHERE clauses safely
  def search(minPop: Option[Int], namePrefix: Option[String]): ConnectionIO[List[String]] =
    import doobie.util.fragments.whereAndOpt
    val q = fr"SELECT name FROM city" ++
      whereAndOpt(minPop.map(p => fr"population >= $p"), namePrefix.map(n => fr"name LIKE ${n + "%"}")) ++
      fr"ORDER BY name"
    q.query[String].to[List]

  // Joins map to tuples (or case classes)
  val largestCityPerCountry: ConnectionIO[List[(String, String, Int)]] =
    sql"""SELECT co.name, ci.name, ci.population
          FROM city ci JOIN country co ON ci.country_code = co.code
          WHERE ci.population = (SELECT MAX(population) FROM city WHERE country_code = co.code)
          ORDER BY co.name""".query[(String, String, Int)].to[List]

  def run: IO[Unit] =
    val setup = for
      _ <- createSchema
      n <- List(Country("PRT", "Portugal", 10_300_000), Country("JPN", "Japan", 124_000_000)).traverse(insertCountry)
      m <- insertCities(List(("Lisbon", "PRT", 545_000), ("Porto", "PRT", 232_000), ("Braga", "PRT", 193_000),
                             ("Tokyo", "JPN", 13_960_000), ("Osaka", "JPN", 2_750_000)))
    yield (n.sum, m)

    for
      counts <- setup.transact(xa)                         // one transaction
      _ <- IO.println(s"=== inserted ${counts._1} countries and ${counts._2} cities ===")
      pt <- citiesIn("PRT").transact(xa)
      _ <- IO.println(s"  cities in PRT: ${pt.map(c => s"${c.name} (${c.population})").mkString(", ")}")
      jp <- findCountry("JPN").transact(xa)
      no <- findCountry("XXX").transact(xa)
      _ <- IO.println(s"  findCountry: $jp / $no")
      s1 <- search(Some(500_000), None).transact(xa)
      s2 <- search(None, Some("Os")).transact(xa)
      s3 <- search(None, None).transact(xa)
      _ <- IO.println(s"  dynamic search: big=$s1 os*=$s2 all=$s3")
      lc <- largestCityPerCountry.transact(xa)
      _ <- IO.println(s"  largest city per country: $lc")

      _ <- IO.println("\n=== transactions roll back on failure ===")
      r <- (insertCountry(Country("ESP", "Spain", 48_000_000)) *> insertCountry(Country("PRT", "duplicate key!", 0)))
             .transact(xa).attempt
      _ <- IO.println(s"  failed: ${r.left.map(_.getClass.getSimpleName)}")
      es <- findCountry("ESP").transact(xa)
      _ <- IO.println(s"  Spain inserted before the failure? $es  (rolled back)")

      _ <- IO.println("\n=== SQL injection is impossible with interpolation ===")
      evil = "PRT' OR '1'='1"
      inj <- findCountry(evil).transact(xa)
      _ <- IO.println(s"  findCountry(\"$evil\") → $inj   (parameter bound, not concatenated)")
      _ <- IO.println(s"  the generated SQL: ${sql"SELECT * FROM country WHERE code = $evil".query[String].sql}")
    yield ()
