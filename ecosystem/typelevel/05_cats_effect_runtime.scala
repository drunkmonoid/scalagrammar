//> using scala 3.9.0
//> using dep org.typelevel::cats-effect:3.7.1
// See also: ecosystem/typelevel/03_cats_effect.scala
// Run: scala-cli run ecosystem/typelevel/05_cats_effect_runtime.scala
//
// Cats Effect runtime utilities: IOLocal, Supervisor, and Dispatcher.

package ecosystem.catseffect.runtime

import cats.effect.*
import cats.effect.std.{Dispatcher, Supervisor}
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import scala.concurrent.duration.*
import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("IOLocal: fiber-local context isolation")
  val ioLocalDemo = for
    local <- IOLocal("initial")
    results <- (
      local.set("fiber-1") *> IO.sleep(20.millis) *> local.get,
      local.set("fiber-2") *> IO.sleep(20.millis) *> local.get
    ).parTupled
  yield results

  val (f1, f2) = ioLocalDemo.unsafeRunSync()
  println(s"fiber1: $f1, fiber2: $f2") // fiber1: fiber-1, fiber2: fiber-2

  section("Supervisor: structured lifecycle and automatic cancellation")
  val supervisorDemo = Supervisor[IO].use { supervisor =>
    for
      // Start a fiber with a long sleep that would print if not cancelled
      _ <- supervisor.supervise(IO.sleep(3.seconds) *> IO.println("  delayed supervisor print"))
      _ <- IO.sleep(50.millis)
    yield ()
  } *> IO.println("supervisor scope ended") // supervisor scope ended

  supervisorDemo.unsafeRunSync()

  section("Dispatcher: bridging effectful code to unmanaged / impure callbacks")
  val dispatcherDemo = Dispatcher.parallel[IO].use { dispatcher =>
    for
      deferred <- Deferred[IO, String]
      _ <- IO {
        // Simulating an external callback thread safely invoking IO via Dispatcher
        dispatcher.unsafeRunAndForget(deferred.complete("delivered-via-dispatcher"))
      }
      value <- deferred.get
    yield value
  }

  val delivered = dispatcherDemo.unsafeRunSync()
  println(delivered) // delivered-via-dispatcher

  section("Compile-time safety with typeCheckErrors")
  // IOLocal operations return IO[A], not raw values synchronously:
  doesNotCompile("""
    import cats.effect.*
    val bad: String = IOLocal("val").get
  """)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
