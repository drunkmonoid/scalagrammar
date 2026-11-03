//> using scala 3.9.0
//> using dep org.typelevel::cats-effect:3.7.1
// Run: scala-cli run ecosystem/typelevel/03_cats_effect.scala
//
// cats-effect: a purely functional runtime for asynchronous, concurrent programs.
//   IO[A]      — a DESCRIPTION of a computation producing A (lazy, referentially transparent)
//   Fibers     — lightweight green threads (millions are fine), cancellable
//   Resource   — acquire/release that is safe under errors AND cancellation
//   Ref / Deferred / Queue / Semaphore — concurrency primitives
//   IOApp      — the entry point that owns the runtime (thread pools, signal handling)

package ecosystem.catseffect

import cats.effect.*
import cats.effect.std.{Queue, Semaphore, Random}
import cats.syntax.all.*
import scala.concurrent.duration.*

object Main extends IOApp.Simple:

  def section(title: String): IO[Unit] = IO.println(s"\n=== $title ===")

  // ---------- 1. IO is lazy and referentially transparent ----------
  val laziness: IO[Unit] =
    val hello = IO.println("  hello")       // nothing printed yet: just a value
    for
      _ <- section("IO is a value: running it twice prints twice")
      _ <- hello
      _ <- hello
      // Compare with Future, which runs once at construction time (lessons/09-concurrency).
      r <- IO.pure(21).map(_ * 2)
      _ <- IO.println(s"  result $r")
    yield ()

  // ---------- 2. errors ----------
  val errors: IO[Unit] =
    val boom: IO[Int] = IO.raiseError(RuntimeException("boom"))
    for
      _ <- section("Error handling")
      a <- boom.handleError(_ => -1)
      b <- boom.attempt                                  // IO[Either[Throwable, Int]]
      c <- IO("x".toInt).recover { case _: NumberFormatException => 0 }
      _ <- IO.println(s"  handleError=$a attempt=${b.left.map(_.getMessage)} recover=$c")
      _ <- boom.guarantee(IO.println("  finalizer runs even on error")).void.voidError
      _ <- IO.println("  (voidError swallowed the failure)")
    yield ()

  // ---------- 3. resources ----------
  def connection(name: String): Resource[IO, String] =
    Resource.make(IO.println(s"  open $name").as(name))(n => IO.println(s"  close $n"))

  val resources: IO[Unit] =
    val both = (connection("db"), connection("cache")).tupled    // released in reverse order
    section("Resource: safe acquire/release") *>
      both.use { case (db, cache) => IO.println(s"  using $db and $cache") } *>
      connection("failing").use(_ => IO.raiseError[Unit](RuntimeException("oops"))).attempt.void

  // ---------- 4. fibers, parallelism, racing ----------
  def task(name: String, d: FiniteDuration): IO[String] =
    IO.sleep(d) *> IO.println(s"  $name finished after $d").as(name)

  val concurrency: IO[Unit] = for
    _  <- section("Fibers & parallelism")
    t0 <- IO.monotonic
    r  <- (task("a", 200.millis), task("b", 200.millis)).parTupled    // run in parallel
    t1 <- IO.monotonic
    _  <- IO.println(s"  parTupled → $r in ~${(t1 - t0).toMillis} ms (not 400)")
    xs <- List(1, 2, 3).parTraverse(i => IO.sleep(50.millis).as(i * i))
    _  <- IO.println(s"  parTraverse → $xs")
    w  <- IO.race(task("slow", 500.millis), task("fast", 50.millis))
    _  <- IO.println(s"  race winner → $w   (the loser is CANCELLED)")
    f  <- (IO.sleep(100.millis) *> IO.pure(42)).start                 // fork a fiber
    o  <- f.join                                                      // Outcome
    v  <- o.embedNever
    _  <- IO.println(s"  start/join → $v")
    t  <- task("too slow", 1.second).timeout(100.millis).attempt
    _  <- IO.println(s"  timeout → ${t.left.map(_.getClass.getSimpleName)}")
  yield ()

  // ---------- 5. cancellation ----------
  val cancellation: IO[Unit] = for
    _ <- section("Cancellation is safe")
    f <- (IO.println("  working...") *> IO.sleep(1.second) *> IO.println("  never printed"))
           .onCancel(IO.println("  cleanup on cancel"))
           .start
    _ <- IO.sleep(100.millis)
    _ <- f.cancel
    o <- f.join
    _ <- IO.println(s"  outcome: $o")
  yield ()

  // ---------- 6. concurrency primitives ----------
  val primitives: IO[Unit] = for
    _       <- section("Ref, Deferred, Queue, Semaphore")
    counter <- Ref.of[IO, Int](0)
    _       <- List.fill(1000)(counter.update(_ + 1)).parSequence_   // atomic updates
    n       <- counter.get
    _       <- IO.println(s"  Ref after 1000 parallel increments: $n")
    gate    <- Deferred[IO, String]
    waiter  <- gate.get.flatMap(v => IO.println(s"  Deferred delivered: $v")).start
    _       <- IO.sleep(50.millis) *> gate.complete("go!")
    _       <- waiter.join
    q       <- Queue.bounded[IO, Int](2)
    prod    <- List.range(1, 6).traverse_(i => q.offer(i)).start         // back-pressured
    got     <- List.fill(5)(q.take).sequence
    _       <- prod.join
    _       <- IO.println(s"  Queue consumed: $got")
    sem     <- Semaphore[IO](2)
    active  <- Ref.of[IO, Int](0)
    maxSeen <- Ref.of[IO, Int](0)
    job      = sem.permit.surround(
                 active.updateAndGet(_ + 1).flatMap(a => maxSeen.update(_ max a)) *>
                 IO.sleep(20.millis) *> active.update(_ - 1))
    _       <- List.fill(10)(job).parSequence_
    m       <- maxSeen.get
    _       <- IO.println(s"  Semaphore(2): max concurrent = $m")
  yield ()

  // ---------- 7. a tiny "real" program ----------
  val program: IO[Unit] = for
    _    <- section("Putting it together: retry with backoff")
    rnd  <- Random.scalaUtilRandomSeedInt[IO](7)
    flaky = rnd.nextIntBounded(3).flatMap(n => if n == 0 then IO.pure("success") else IO.raiseError(RuntimeException(s"failure $n")))
    r    <- retry(flaky, maxAttempts = 10, delay = 10.millis)
    _    <- IO.println(s"  $r")
  yield ()

  def retry[A](io: IO[A], maxAttempts: Int, delay: FiniteDuration): IO[A] =
    io.handleErrorWith { e =>
      if maxAttempts <= 1 then IO.raiseError(e)
      else IO.println(s"  ${e.getMessage}, retrying in $delay") *> IO.sleep(delay) *> retry(io, maxAttempts - 1, delay * 2)
    }

  def run: IO[Unit] =
    laziness *> errors *> resources *> concurrency *> cancellation *> primitives *> program
