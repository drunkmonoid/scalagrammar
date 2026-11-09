//> using scala 3.9.0
//> using dep com.github.ghostdogpr::caliban:2.9.2
//> using dep dev.zio::zio:2.1.27
// See also: ecosystem/zio/01_zio.scala
// Run: scala-cli run ecosystem/graphql/01_caliban.scala
//
// In-process GraphQL execution with Caliban (no HTTP server required):
// 1. Schema derivation from case classes using RootResolver and graphQL
// 2. Query execution with typed arguments and nested selection sets
// 3. Schema Definition Language (SDL) generation via api.render
// 4. Schema validation and field resolution error handling

package ecosystem.graphql.calibandemo

import caliban.*
import caliban.schema.Annotations.GQLDescription
import zio.*

// Domain models
case class Character(
  @GQLDescription("Full name of the historical figure")
  name: String,
  age: Int,
  role: String
)

case class CharacterArgs(name: String)

case class Queries(
  @GQLDescription("Retrieve all registered characters")
  characters: List[Character],
  @GQLDescription("Find a single character by exact name")
  character: CharacterArgs => Option[Character]
)

object CalibanApp extends ZIOAppDefault:

  val sampleCharacters = List(
    Character("Ada Lovelace", 36, "Mathematician & First Programmer"),
    Character("Alan Turing", 41, "Father of Theoretical Computer Science"),
    Character("Grace Hopper", 85, "Computer Scientist & Naval Officer")
  )

  val queries = Queries(
    characters = sampleCharacters,
    character = args => sampleCharacters.find(_.name.equalsIgnoreCase(args.name))
  )

  val api = graphQL(RootResolver(queries))

  def run =
    for
      _ <- Console.printLine("=== 1. Rendered GraphQL SDL ===")
      sdl = api.render
      _ <- Console.printLine(sdl)

      interpreter <- api.interpreter

      _ <- Console.printLine("\n=== 2. Valid GraphQL query execution ===")
      validQuery =
        """{
          |  characters {
          |    name
          |    role
          |  }
          |}""".stripMargin
      res1 <- interpreter.execute(validQuery)
      _ <- Console.printLine(s"Query result data:\n${res1.data}")
      _ <- ZIO.attempt(assert(res1.errors.isEmpty))

      _ <- Console.printLine("\n=== 3. Query with argument ===")
      queryWithArg =
        """{
          |  character(name: "Ada Lovelace") {
          |    name
          |    age
          |    role
          |  }
          |}""".stripMargin
      res2 <- interpreter.execute(queryWithArg)
      _ <- Console.printLine(s"Argument query result data:\n${res2.data}")
      _ <- ZIO.attempt(assert(res2.errors.isEmpty))

      _ <- Console.printLine("\n=== 4. Invalid field query (validation error) ===")
      invalidQuery =
        """{
          |  characters {
          |    name
          |    nonExistentField
          |  }
          |}""".stripMargin
      res3 <- interpreter.execute(invalidQuery)
      _ <- Console.printLine(s"Validation errors:\n${res3.errors.map(_.getMessage).mkString("\n")}")
      _ <- ZIO.attempt(assert(res3.errors.nonEmpty))
      _ <- Console.printLine("\nCaliban in-process GraphQL tests completed successfully.")
    yield ()
