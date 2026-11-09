//> using scala 3.9.0
//> using dep co.fs2::fs2-core:3.14.0
// See also: ecosystem/typelevel/04_fs2_streams.scala
// Run: scala-cli run ecosystem/typelevel/06_fs2_pulls.scala
//
// fs2 internals: building low-level streams with Pull, Chunk boundaries, and interruptScope.

package ecosystem.fs2pulls

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.{Chunk, Pull, Stream}
import scala.concurrent.duration.*
import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("Emitting elements with Pull")
  // Pull is the fundamental, low-level coroutine model behind fs2 Stream.
  // It emits chunks or single elements and can terminate explicitly:
  val emitThree: Pull[fs2.Pure, Int, Unit] =
    for
      _ <- Pull.output1(1)
      _ <- Pull.output1(2)
      _ <- Pull.output1(3)
    yield ()

  val pullList: List[Int] = emitThree.stream.toList
  println(pullList) // List(1, 2, 3)

  section("Chunk boundaries")
  // fs2 operations run over Chunks for high throughput. Creating streams with explicit
  // chunk sizes exposes chunk boundaries rather than a flattened element sequence:
  val s = Stream.chunk(Chunk(10, 20, 30)) ++ Stream.chunk(Chunk(40, 50))
  val chunks: List[Chunk[Int]] = s.chunks.toList
  println(chunks) // List(Chunk(10, 20, 30), Chunk(40, 50))

  section("Stream interruption with interruptScope")
  // A stream that would otherwise run forever is scoped and safely interrupted:
  val infiniteEffectful = Stream.repeatEval(IO.sleep(10.millis))
    .interruptScope
    .interruptAfter(100.millis)

  infiniteEffectful.compile.drain.unsafeRunSync()
  println("stream stopped") // stream stopped

  section("Compile-time safety with typeCheckErrors")
  // Pull cannot directly invoke Stream combinators without converting via .stream:
  doesNotCompile("Pull.output1(1).toList")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
