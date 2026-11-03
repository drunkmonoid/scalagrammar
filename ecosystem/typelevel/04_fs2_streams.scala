//> using scala 3.9.0
//> using dep co.fs2::fs2-core:3.14.0
//> using dep co.fs2::fs2-io:3.14.0
// Run: scala-cli run ecosystem/typelevel/04_fs2_streams.scala
//
// fs2: functional, effectful, constant-memory streaming on top of cats-effect.
//   Stream[F, O] — a lazy stream of O values whose evaluation may perform effects in F (IO).
//   Pure streams (F = Pure) behave like lazy lists; effectful ones are compiled to IO.
//   Streams are pull-based, chunked (for speed), resource-safe and concurrent.

package ecosystem.fs2demo

import cats.effect.*
import cats.syntax.all.*
import fs2.*
import fs2.io.file.{Files, Path}
import scala.concurrent.duration.*

object Main extends IOApp.Simple:

  def section(title: String): IO[Unit] = IO.println(s"\n=== $title ===")

  // ---------- pure streams ----------
  val pure: IO[Unit] = section("Pure streams") *> IO {
    val s = Stream(1, 2, 3) ++ Stream.range(4, 7)
    println(s.toList)
    println(s.map(_ * 2).filter(_ > 5).toList)
    println(Stream.iterate(1)(_ * 2).take(10).toList)                 // infinite, but lazy
    println(Stream("a", "b", "c").zipWithIndex.toList)
    println(Stream.range(1, 10).chunkN(3).map(_.toList).toList)
    println(Stream(1, 1, 2, 2, 2, 3).changes.toList)                   // drop consecutive duplicates
    println(Stream.range(1, 6).scan(0)(_ + _).toList)
    println(Stream.emits(List("x", "y")).intersperse(",").toList.mkString)
    println(Stream(1, 2, 3).repeat.take(7).toList)
  }

  // ---------- effects ----------
  val effects: IO[Unit] = for
    _  <- section("Effectful streams: evalMap, compile")
    xs <- Stream.range(1, 4)
            .evalMap(i => IO.println(s"  processing $i").as(i * 10))  // effect per element
            .compile.toList                                            // run → IO[List[Int]]
    _  <- IO.println(s"  result: $xs")
    n  <- Stream.repeatEval(IO(scala.util.Random.nextInt(100))).take(1000).compile.count
    _  <- IO.println(s"  counted $n random numbers without holding them in memory")
    s  <- Stream.range(1, 1_000_001).covary[IO].fold(0L)(_ + _).compile.lastOrError
    _  <- IO.println(s"  sum of a million elements in constant memory: $s")
  yield ()

  // ---------- resource safety ----------
  val resources: IO[Unit] =
    val resource = Stream.bracket(IO.println("  acquire").as("conn"))(_ => IO.println("  release"))
    section("Resource safety") *>
      resource.flatMap(c => Stream(1, 2, 3).map(i => s"$c-$i")).evalTap(x => IO.println(s"  $x")).compile.drain *>
      resource.flatMap(_ => Stream.raiseError[IO](RuntimeException("mid-stream failure")))
        .compile.drain.attempt.flatMap(r => IO.println(s"  failed with ${r.left.map(_.getMessage)} — still released"))

  // ---------- time & concurrency ----------
  val concurrency: IO[Unit] = for
    _ <- section("Concurrency & time")
    t0 <- IO.monotonic
    r  <- Stream.range(1, 9).covary[IO]
            .parEvalMap(4)(i => IO.sleep(100.millis).as(i * i))      // 4 at a time, ORDER KEPT
            .compile.toList
    t1 <- IO.monotonic
    _  <- IO.println(s"  parEvalMap(4) → $r in ~${(t1 - t0).toMillis} ms (8 × 100 ms / 4)")
    m  <- (Stream.awakeEvery[IO](30.millis).as("tick").take(3) merge
           Stream.awakeEvery[IO](45.millis).as("TOCK").take(2)).compile.toList
    _  <- IO.println(s"  merge (interleaved by time) → $m")
    g  <- Stream.awakeEvery[IO](10.millis).zipWithIndex.map(_._2)
            .groupWithin(5, 1.second).take(3).map(_.toList).compile.toList
    _  <- IO.println(s"  groupWithin(5, 1s) → $g")
    i  <- Stream.awakeEvery[IO](20.millis).as(1).scan(0)(_ + _)
            .interruptAfter(110.millis).compile.last
    _  <- IO.println(s"  interruptAfter → last count ${i.getOrElse(0)}")
  yield ()

  // ---------- files ----------
  val files: IO[Unit] = for
    _   <- section("Files with fs2-io")
    tmp <- Files[IO].createTempFile
    _   <- Stream.emits(List("alpha", "beta", "gamma", "delta"))
             .intersperse("\n").through(text.utf8.encode)
             .through(Files[IO].writeAll(tmp)).compile.drain
    out <- Files[IO].readAll(tmp).through(text.utf8.decode).through(text.lines)
             .filter(_.contains("a")).map(_.toUpperCase).compile.toList
    _   <- IO.println(s"  lines containing 'a', upper-cased: $out")
    _   <- Files[IO].delete(tmp)
  yield ()

  // ---------- pipes ----------
  // A Pipe[F, I, O] is just Stream[F, I] => Stream[F, O]: reusable stream transformations
  def runningAverage[F[_]]: Pipe[F, Double, Double] =
    _.scan((0.0, 0)) { case ((sum, n), x) => (sum + x, n + 1) }.drop(1).map((s, n) => s / n)

  val pipes: IO[Unit] = section("Pipes") *>
    IO.println(Stream(2.0, 4.0, 6.0, 8.0).through(runningAverage).toList)

  def run: IO[Unit] = pure *> effects *> resources *> concurrency *> files *> pipes
