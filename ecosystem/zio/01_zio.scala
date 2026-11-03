//> using scala 3.9.0
//> using dep dev.zio::zio:2.1.27
//> using dep dev.zio::zio-streams:2.1.27
// Run: scala-cli run ecosystem/zio/01_zio.scala
//
// ZIO: an effect system like cats-effect, with one key difference in the TYPE:
//   ZIO[R, E, A] — needs an environment R, may fail with a TYPED error E, or succeed with A
//   aliases: UIO[A] = ZIO[Any, Nothing, A], Task[A] = ZIO[Any, Throwable, A], IO[E, A], URIO[R, A]
// Dependencies are provided as ZLayers; ZIO ships its own fibers, STM, streams, schedules.

package ecosystem.ziodemo

import zio.*
import zio.stream.*
import zio.stm.*

// ---------- typed errors ----------
enum AppError:
  case NotFound(id: Int)
  case Invalid(msg: String)

// ---------- services and layers (dependency injection) ----------
trait UserRepo:
  def find(id: Int): IO[AppError, String]

final case class InMemoryUserRepo(data: Map[Int, String]) extends UserRepo:
  def find(id: Int): IO[AppError, String] =
    ZIO.fromOption(data.get(id)).orElseFail(AppError.NotFound(id))

object InMemoryUserRepo:
  val layer: ULayer[UserRepo] = ZLayer.succeed(InMemoryUserRepo(Map(1 -> "ada", 2 -> "bob")))

trait Greeter:
  def greet(id: Int): IO[AppError, String]

final case class GreeterLive(repo: UserRepo) extends Greeter:
  def greet(id: Int): IO[AppError, String] = repo.find(id).map(n => s"Hello, ${n.capitalize}!")

object GreeterLive:
  val layer: URLayer[UserRepo, Greeter] = ZLayer.fromFunction(GreeterLive.apply)   // needs a UserRepo

object Main extends ZIOAppDefault:

  def section(title: String): UIO[Unit] = Console.printLine(s"\n=== $title ===").orDie

  val basics = for
    _ <- section("Effects are values")
    hello = Console.printLine("  hello").orDie
    _ <- hello *> hello                                  // runs twice
    n <- ZIO.succeed(21).map(_ * 2)
    _ <- Console.printLine(s"  $n").orDie
    t <- ZIO.attempt("42".toInt)                         // Task[Int]: captures exceptions
    _ <- Console.printLine(s"  attempt → $t").orDie
  yield ()

  val typedErrors = for
    _ <- section("Typed errors")
    r <- InMemoryUserRepo(Map.empty).find(7).either       // IO[Nothing, Either[AppError, String]]
    _ <- Console.printLine(s"  either → $r").orDie
    v <- ZIO.fail(AppError.Invalid("bad")).catchAll {
           case AppError.Invalid(m)  => ZIO.succeed(s"recovered from '$m'")
           case AppError.NotFound(i) => ZIO.succeed(s"missing $i")
         }
    _ <- Console.printLine(s"  catchAll → $v").orDie
    // Defects (unexpected exceptions) are separate from typed failures:
    d <- ZIO.succeed(1 / 0).sandbox.either
    _ <- Console.printLine(s"  defect captured as Cause: ${d.left.map(_.prettyPrint.linesIterator.next())}").orDie
    // Validation that accumulates errors:
    acc <- ZIO.validate(List(1, -2, -3))(i => if i > 0 then ZIO.succeed(i) else ZIO.fail(s"$i is negative")).either
    _ <- Console.printLine(s"  validate → $acc").orDie
  yield ()

  val layers = for
    _ <- section("Dependency injection with ZLayer")
    g <- ZIO.serviceWithZIO[Greeter](_.greet(1))
    _ <- Console.printLine(s"  $g").orDie
    e <- ZIO.serviceWithZIO[Greeter](_.greet(99)).flip
    _ <- Console.printLine(s"  failure → $e").orDie
  yield ()

  val concurrency = for
    _  <- section("Fibers, parallelism, racing, timeouts")
    ab <- (ZIO.sleep(200.millis).as("a") <&> ZIO.sleep(200.millis).as("b")).timed   // parallel zip
    _  <- Console.printLine(s"  <&> → ${ab._2} in ${ab._1.toMillis} ms").orDie
    xs <- ZIO.foreachPar(List(1, 2, 3))(i => ZIO.sleep(50.millis).as(i * i))
    _  <- Console.printLine(s"  foreachPar → $xs").orDie
    w  <- ZIO.sleep(1.second).as("slow").race(ZIO.sleep(50.millis).as("fast"))
    _  <- Console.printLine(s"  race → $w").orDie
    to <- ZIO.sleep(1.second).as("done").timeout(100.millis)
    _  <- Console.printLine(s"  timeout → $to").orDie
    f  <- (ZIO.sleep(1.second) *> Console.printLine("never")).onInterrupt(Console.printLine("  interrupted, cleaning up").orDie).fork
    _  <- ZIO.sleep(50.millis) *> f.interrupt
  yield ()

  val stateAndSchedules = for
    _   <- section("Ref, STM and Schedules")
    ref <- Ref.make(0)
    _   <- ZIO.foreachParDiscard(1 to 1000)(_ => ref.update(_ + 1))
    v   <- ref.get
    _   <- Console.printLine(s"  Ref: $v").orDie
    // STM: composable atomic transactions
    from <- TRef.make(100).commit
    to   <- TRef.make(0).commit
    _    <- (from.update(_ - 30) *> to.update(_ + 30)).commit
    bal  <- (from.get <*> to.get).commit
    _    <- Console.printLine(s"  STM transfer → $bal").orDie
    // Schedules: retry/repeat policies as values
    attempts <- Ref.make(0)
    flaky     = attempts.updateAndGet(_ + 1).flatMap(n => if n < 3 then ZIO.fail(s"attempt $n failed") else ZIO.succeed(s"ok on attempt $n"))
    r        <- flaky.retry(Schedule.exponential(10.millis) && Schedule.recurs(5))
    _        <- Console.printLine(s"  retry with exponential backoff → $r").orDie
    ticks    <- Ref.make(0)
    _        <- ticks.update(_ + 1).repeat(Schedule.spaced(10.millis) && Schedule.recurs(4))
    t        <- ticks.get
    _        <- Console.printLine(s"  repeat 4 more times → $t runs").orDie
  yield ()

  val resources = for
    _ <- section("Resources with Scope")
    _ <- ZIO.scoped {
           ZIO.acquireRelease(Console.printLine("  open").orDie.as("handle"))(_ => Console.printLine("  close").orDie)
             .flatMap(h => Console.printLine(s"  using $h").orDie)
         }
  yield ()

  val streams = for
    _ <- section("ZStream")
    r <- ZStream.range(1, 11).filter(_ % 2 == 0).map(_ * 10).runCollect
    _ <- Console.printLine(s"  $r").orDie
    s <- ZStream.iterate(1)(_ * 2).take(10).runSum
    _ <- Console.printLine(s"  sum of first 10 powers of two: $s").orDie
    g <- ZStream.fromIterable(1 to 7).grouped(3).map(_.toList).runCollect
    _ <- Console.printLine(s"  grouped: $g").orDie
  yield ()

  def run =
    (basics *> typedErrors *> layers *> concurrency *> stateAndSchedules *> resources *> streams)
      .provide(InMemoryUserRepo.layer, GreeterLive.layer)       // wire the dependency graph
