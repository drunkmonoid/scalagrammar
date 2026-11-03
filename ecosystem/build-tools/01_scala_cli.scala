//> using scala 3.9.0
//> using dep com.lihaoyi::pprint:0.9.0
//> using options -deprecation -feature -Wunused:all
//> using javaOpt -Xmx512m
//> using resourceDir ./resources
// Run: scala-cli run ecosystem/build-tools/01_scala_cli.scala
//
// scala-cli: the official `scala` command since Scala 3.5. Everything a small project needs
// lives in `//> using` directives at the top of a file (or in a project.scala):
//
//   //> using scala 3.9.0                  Scala version (also 2.13.x, 2.12.x)
//   //> using jvm 21                       JDK to download/use
//   //> using dep org::name:version        library dependency (:: = Scala artifact, : = Java)
//   //> using test.dep org::name:version   test-only dependency (for *.test.scala files)
//   //> using options -Wunused:all         compiler options
//   //> using javaOpt -Xmx1g               JVM options when running
//   //> using mainClass pkg.Main           which main to run
//   //> using file other.scala             include another source file
//   //> using resourceDir ./resources      classpath resources
//   //> using platform js | native         Scala.js / Scala Native instead of the JVM
//   //> using repository https://...       extra Maven repositories
//   //> using exclude ...                  exclude files from a directory build
//
// Useful commands:
//   scala-cli run file.scala -- arg1 arg2      compile & run (args after --)
//   scala-cli repl file.scala                  REPL with the file's classpath (+ --dep for more)
//   scala-cli test dir/                        run *.test.scala files with MUnit/ScalaTest/...
//   scala-cli compile --watch file.scala       recompile on change (also run --watch)
//   scala-cli fmt .                            format with scalafmt
//   scala-cli package file.scala -o app        runnable launcher / --assembly fat JAR / --native-image
//   scala-cli setup-ide .                      generate BSP config for Metals / IntelliJ
//   scala-cli export --sbt / --mill .          convert to a full build tool
//   scala-cli --power publish local .          publish to ~/.ivy2/local
//   scala-cli doc .                            generate Scaladoc
//   scala-cli clean .                          remove .scala-build
//   scala-cli -e 'println(1 + 1)'              evaluate a snippet
//   scala-cli shebang script.sc                scripts: `#!/usr/bin/env -S scala-cli shebang`

package ecosystem.scalacli

@main def run(): Unit =
  println("=== a dependency, fetched by coursier from Maven Central ===")
  case class Config(name: String, ports: List[Int], nested: Map[String, Option[Double]])
  pprint.pprintln(Config("demo", List(80, 443), Map("ratio" -> Some(0.5), "none" -> None)))

  println("\n=== a classpath resource from //> using resourceDir ===")
  val res = Option(getClass.getResourceAsStream("/greeting.txt"))
  println(res.map(scala.io.Source.fromInputStream(_).mkString.trim).getOrElse("<missing>"))

  println("\n=== JVM options from //> using javaOpt ===")
  println(s"max heap ≈ ${Runtime.getRuntime.maxMemory / 1024 / 1024} MB")
