// sbt: the most widely used Scala build tool.
//
//   cd ecosystem/build-tools/sbt-project
//   sbt                 # interactive shell (fast: the JVM stays warm)
//   > compile           # compile all modules
//   > test              # run tests (MUnit here)
//   > app/run           # run the app module
//   > ~core/test        # re-run on every file change
//   > console           # REPL with the project classpath
//   > dependencyTree    # show transitive dependencies
//   > show core/libraryDependencies
//   > inspect tree compile
//   > reload            # after editing build.sbt
//
// Key ideas: settings (`:=` computed once), tasks (`.value` dependencies, run on demand),
// scopes (project / configuration (Compile, Test) / task), and `%%` which appends the
// Scala binary version to an artifact name (cats-core → cats-core_3).

ThisBuild / scalaVersion := "3.9.0"
ThisBuild / organization := "dev.scalagrammar"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature", "-Wunused:all")

lazy val core = project
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.12.0",
      "org.scalameta" %% "munit"     % "1.0.3" % Test,
    ),
  )

lazy val app = project
  .dependsOn(core)                       // app sees core's classes (and its dependencies)
  .settings(
    name := "app",
    Compile / run / fork := true,        // run in a separate JVM
    // A custom task: `sbt app/hello`
    hello := {
      val log = streams.value.log
      log.info(s"Hello from a custom task, Scala ${scalaVersion.value}")
    },
  )

lazy val hello = taskKey[Unit]("Prints a greeting")

lazy val root = (project in file("."))
  .aggregate(core, app)                  // commands on root run on both modules
  .settings(publish / skip := true)
