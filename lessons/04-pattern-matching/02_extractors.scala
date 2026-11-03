//> using scala 3.9.0
// Run: scala-cli run lessons/04-pattern-matching/02_extractors.scala
//
// Extractors: custom patterns via unapply / unapplySeq. The four result shapes:
//   Boolean                 → boolean pattern  `case Even() =>`
//   Option[T] / Some[T]     → single value     `case Email(user, host)` when T is a tuple
//   any type with isEmpty/get ("name-based") → no allocation of Option
//   Product (with _1.._N)   → fixed arity without Option (Scala 3)
// unapplySeq returns a sequence (or Option of one) for variable-arity patterns.

package patterns.extractors

// ---------- Option-returning extractor ----------
object Email:
  def unapply(s: String): Option[(String, String)] =
    s.split("@") match
      case Array(user, host) if user.nonEmpty && host.contains('.') => Some((user, host))
      case _                                                         => None

// ---------- single value ----------
object PositiveInt:
  def unapply(s: String): Option[Int] = s.toIntOption.filter(_ > 0)

// ---------- boolean extractor ----------
object Even:
  def unapply(n: Int): Boolean = n % 2 == 0

// ---------- variable arity ----------
object Words:
  def unapplySeq(s: String): Option[Seq[String]] =
    val ws = s.trim.split("\\s+").toSeq.filter(_.nonEmpty)
    if ws.isEmpty then None else Some(ws)

// ---------- fixed prefix + variable tail ----------
object Path:
  // first element is the "root", then any number of segments
  def unapplySeq(p: String): Option[(String, Seq[String])] =
    val parts = p.split('/').toList.filter(_.nonEmpty)
    parts match
      case root :: segs => Some((root, segs))
      case Nil          => None

// ---------- name-based extractor: no Option allocation ----------
final class NameMatch(val value: String):
  def isEmpty: Boolean = value == null
  def get: String = value
object Upper:
  def unapply(s: String): NameMatch = NameMatch(if s.forall(_.isUpper) then s else null)

// ---------- Product-based extractor (Scala 3) ----------
final class Range2(val lo: Int, val hi: Int) extends Product:
  def _1 = lo
  def _2 = hi
  def canEqual(that: Any) = that.isInstanceOf[Range2]
  def productArity = 2
  def productElement(n: Int) = if n == 0 then lo else hi
object Range2:
  def unapply(r: Range2): Range2 = r            // returns the product itself: always matches

// ---------- extractors on classes (instance unapply = parameterised extractors) ----------
class DivisibleBy(n: Int):
  def unapply(x: Int): Boolean = x % n == 0
val By3 = DivisibleBy(3)
val By5 = DivisibleBy(5)

// ---------- extractor that also transforms ----------
object Celsius:
  def unapply(s: String): Option[Double] =
    if s.endsWith("C") then s.dropRight(1).toDoubleOption else None
object Fahrenheit:
  def unapply(s: String): Option[Double] =
    if s.endsWith("F") then s.dropRight(1).toDoubleOption.map(f => (f - 32) * 5 / 9) else None
object Temperature:
  def unapply(s: String): Option[Double] = Celsius.unapply(s).orElse(Fahrenheit.unapply(s))

@main def run(): Unit =
  section("Option extractors")
  for s <- List("ada@lovelace.org", "bad@", "x") do
    s match
      case Email(user, host) => println(s"  user=$user host=$host")
      case _                 => println(s"  not an email: $s")
  "42" match
    case PositiveInt(n) => println(s"  positive $n")
    case _              => ()

  section("Boolean extractors")
  for n <- 1 to 4 do
    n match
      case Even() => println(s"  $n even")
      case _      => println(s"  $n odd")

  section("unapplySeq")
  "  the quick brown fox " match
    case Words(first, second, rest*) => println(s"  $first / $second / rest=$rest")
    case _                           => ()
  "/usr/local/bin" match
    case Path(root, segs*) => println(s"  root=$root segs=$segs")
    case _                 => ()

  section("Name-based & product-based (allocation-free)")
  List("ABC", "AbC").foreach {
    case Upper(u) => println(s"  upper: $u")
    case other    => println(s"  not upper: $other")
  }
  Range2(1, 10) match
    case Range2(lo, hi) => println(s"  lo=$lo hi=$hi")

  section("Parameterised extractors (stable vals holding objects with unapply)")
  def fizz(n: Int) = n match
    case By3() if n % 5 == 0 => "FizzBuzz"
    case By3()               => "Fizz"
    case By5()               => "Buzz"
    case _                   => n.toString
  println((1 to 15).map(fizz).mkString(" "))

  section("Extractors that normalise data")
  // `case Celsius(c) | Fahrenheit(c)` is illegal (alternatives can't bind), so compose
  // extractors into a new one instead:
  for t <- List("100C", "212F", "hot") do
    t match
      case Temperature(c) => println(f"  $t%-5s = $c%.1f°C")
      case _              => println(s"  $t: unknown")

  section("Infix extractors")
  // An extractor with 2-element results can be used infix, like ::
  object ~ :
    def unapply(s: String): Option[(String, String)] =
      val i = s.indexOf('~')
      if i < 0 then None else Some((s.take(i), s.drop(i + 1)))
  "left~right" match
    case l ~ r => println(s"  l=$l r=$r")
    case _     => ()

  section("Extractors in val definitions & for")
  val Email(u, h) = "x@y.com": @unchecked
  println(s"  $u at $h")
  for case Email(user, _) <- List("a@b.c", "nope", "c@d.e") do println(s"  user $user")

def section(title: String): Unit = println(s"\n=== $title ===")
