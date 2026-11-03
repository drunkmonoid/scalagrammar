//> using scala 3.9.0
// Run: scala-cli run lessons/08-errors/01_exceptions_resources.scala
//
// JVM exceptions in Scala: throw/try/catch/finally, NonFatal, custom exceptions,
// no checked exceptions, @throws for Java, and resource management with scala.util.Using.

package errors.exceptions

import scala.util.control.NonFatal
import scala.util.{Using, Try}
import java.io.{Closeable, IOException}

// Custom exceptions: plain classes extending Exception (or RuntimeException)
class ValidationException(msg: String, val field: String) extends Exception(msg)
final case class NotFoundException(id: Int) extends RuntimeException(s"no item $id")

// Lightweight exceptions for control flow: no stack trace
class Signal extends RuntimeException(null, null, false, false)

class Resource(name: String) extends AutoCloseable:
  println(s"  open $name")
  def read(): String = if name == "bad" then throw IOException(s"cannot read $name") else s"data of $name"
  def close(): Unit = println(s"  close $name")

@main def run(): Unit =
  section("throw / try / catch / finally")
  def divide(a: Int, b: Int): Int =
    if b == 0 then throw IllegalArgumentException("division by zero") else a / b
  val r = try divide(10, 0)
  catch
    case e: IllegalArgumentException => println(s"  caught: ${e.getMessage}"); -1
    case e: ArithmeticException      => -2
  finally println("  finally runs always")
  println(r)

  section("Catch clauses are pattern matches")
  def risky(i: Int): String = i match
    case 0 => throw NotFoundException(42)
    case 1 => throw ValidationException("bad email", "email")
    case 2 => throw IOException("disk")
    case _ => "ok"
  for i <- 0 to 3 do
    val res = try risky(i) catch
      case NotFoundException(id)                    => s"not found $id"       // extractor!
      case v: ValidationException if v.field == "email" => s"invalid ${v.field}"
      case e @ (_: IOException | _: IllegalStateException) => s"io-ish: ${e.getMessage}"
    println(s"  $i → $res")

  section("Exceptions are expressions of type Nothing")
  val n: Int = Some(5).getOrElse(throw NoSuchElementException("empty"))   // throw: Nothing <: Int
  println(n)
  val flag = if n > 0 then "positive" else throw IllegalStateException("impossible")
  println(flag)

  section("NonFatal: don't catch what you shouldn't")
  // `case e: Throwable` also catches VirtualMachineError (OOM, StackOverflow),
  // InterruptedException, ControlThrowable (used by boundary/break!) and LinkageError.
  def safely[A](body: => A): Either[Throwable, A] =
    try Right(body)
    catch case NonFatal(e) => Left(e)
  println(safely(1 / 0))
  println(safely("fine"))
  // ⚠ catching Throwable around boundary/break swallows the break:
  import scala.util.boundary, boundary.break
  val broken = boundary:
    try
      break(1)
    catch case _: Throwable => 2       // ⚠ catches the Break exception!
  println(s"  broken boundary returned $broken (expected 1)")

  section("finally pitfalls")
  def f(): Int =
    try 1
    finally println("  finally's value is discarded")
  println(f())
  // ⚠ an exception in finally REPLACES the original one:
  try
    try throw RuntimeException("original")
    finally throw RuntimeException("from finally")
  catch case e: RuntimeException => println(s"  surfaced: ${e.getMessage}")

  section("No checked exceptions (but @throws for Java callers)")
  @throws[IOException]("when the file is unreadable")
  def readFile(): String = throw IOException("nope")
  println(Try(readFile()))

  section("Stack traces and causes")
  val wrapped = RuntimeException("wrapper", IllegalStateException("root cause"))
  println(s"  ${wrapped.getMessage} ← ${wrapped.getCause.getMessage}")
  val sig = Signal()
  println(s"  Signal stack trace length: ${sig.getStackTrace.length}")   // 0: cheap
  // addSuppressed collects secondary failures (used by try-with-resources / Using)

  section("scala.util.Using: try-with-resources")
  val single = Using(Resource("a"))(_.read())     // Try[String]
  println(single)
  val failed = Using(Resource("bad"))(_.read())
  println(failed)
  // Several resources, closed in reverse order:
  val both = Using.Manager { use =>
    val x = use(Resource("x"))
    val y = use(Resource("y"))
    x.read() + " + " + y.read()
  }
  println(both)
  // Using.resource: throws instead of returning Try
  println(Using.resource(Resource("plain"))(_.read()))

  section("Exceptions in lazy vals and object initializers")
  object Broken:
    val v: Int = throw IllegalStateException("init failed")
  for attempt <- 1 to 2 do
    try Broken.v
    catch
      case e: ExceptionInInitializerError => println(s"  access $attempt: ExceptionInInitializerError")
      case e: NoClassDefFoundError        => println(s"  access $attempt: NoClassDefFoundError")
      case e: IllegalStateException       => println(s"  access $attempt: $e")
  // A LOCAL object like Broken is a lazy val: each access retries and rethrows the cause.
  // ⚠ A failed TOP-LEVEL object initializer is a static initializer: the first access throws
  //   ExceptionInInitializerError and every later access throws NoClassDefFoundError.

def section(title: String): Unit = println(s"\n=== $title ===")
