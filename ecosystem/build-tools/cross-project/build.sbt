// Cross-compiling across Scala 2.13 and Scala 3 with sbt
// Run: sbt +test

ThisBuild / crossScalaVersions := Seq("2.13.17", "3.9.0")
ThisBuild / scalaVersion := "3.9.0"

lazy val root = (project in file("."))
  .settings(
    name := "cross-project",
    libraryDependencies += "org.scalameta" %% "munit" % "1.3.6" % Test
  )
