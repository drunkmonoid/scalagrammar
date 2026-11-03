//> using scala 3.9.0
// Run: scala-cli run lessons/01-basics/04_strings.scala
//
// Strings are java.lang.String, enriched by StringOps (they behave like Seq[Char]).

package basics.strings

@main def run(): Unit =
  section("Literals")
  println("tab:\t| quote:\" | backslash:\\ | unicode:\u00e9")
  println("""triple quotes: no escapes processed: \n stays literal, "quotes" fine""")
  val multi =
    """|line one
       |  line two (indent kept after the margin)
       |line three""".stripMargin
  println(multi)
  println("#a\n#b".stripMargin('#'))  // custom margin char
  // ⚠ In triple-quoted strings, \u escapes are NOT processed in Scala 3 (they were in 2.12-).
  println("""\u0041""")             // prints \u0041

  section("String interpolators")
  val name = "Ada"
  val age = 36
  println(s"Hi $name, next year you'll be ${age + 1}")
  println(s"Dollar sign: $$5")        // $$ escapes a dollar
  println(s"Method calls need braces: ${name.toUpperCase}, not $name.toUpperCase")
  // f — printf-style, TYPE CHECKED at compile time
  val pi = math.Pi
  println(f"pi=$pi%.3f  age=$age%05d  name=$name%-6s| hex=${255}%x  sci=${12345.678}%e")
  println(f"percent: 50%%")
  // raw — no escape processing
  println(raw"C:\new\table\$name")   // backslashes literal, but $ still interpolates
  // ⚠ s"\d" is an invalid escape (compile error); use raw or \\d
  println(raw"\d+".r.findFirstIn("abc123"))   // Some(123)

  section("Custom interpolator")
  extension (sc: StringContext)
    def shout(args: Any*): String = sc.s(args*).toUpperCase + "!"
    // Interpolators can return anything, not just strings:
    def json(args: Any*): Map[String, Any] =
      val keys = sc.parts.map(_.replaceAll("[^a-zA-Z]", "")).filter(_.nonEmpty)
      keys.zip(args).toMap
  println(shout"hello $name")         // HELLO ADA!
  println(json"name:$name, age:$age")  // Map(name -> Ada, age -> 36)
  // Interpolators also work in PATTERNS (s-interpolator has an unapplySeq):
  "user:alice:42" match
    case s"user:$user:$id" => println(s"user=$user id=$id")
    case _                 => ()
  // ⚠ the pattern binds Strings only and matches the *shortest* prefix greedily left to right
  "a:b:c" match
    case s"$x:$rest" => println(s"x=$x rest=$rest")   // x=a rest=b:c

  section("Common operations (StringOps)")
  val str = "Hello, World"
  println(str.length)
  println(str(0))                     // 'H' — apply(i) = charAt
  println(str.reverse)
  println(str.toLowerCase)
  println(str.split(", ").toList)     // Array → List
  println(str.contains("World"))
  println(str.startsWith("He"))
  println(str.indexOf('o'))           // 4
  println(str.substring(7))           // World
  println(str.slice(0, 5))
  println(str.take(3) + "|" + str.drop(3) + "|" + str.takeRight(3))
  println(str.filter(_.isUpper))      // HW
  println(str.map(_.toUpper))         // a String again (StringOps.map Char=>Char)
  println(str.map(_.toInt).take(3))   // ⚠ IndexedSeq[Int] — not a String any more
  println(str.count(_ == 'l'))
  println(str.distinct)
  println(str.sorted)
  println(str * 2)
  println("  trim me  ".trim + "|" + "  strip  ".strip + "|")
  println("a,b,,c,".split(",").toList)     // ⚠ List(a, b, , c) — trailing empties dropped!
  println("a,b,,c,".split(",", -1).toList) // List(a, b, , c, ) — keep them
  println("a.b.c".split(".").toList)       // ⚠ List() — split takes a REGEX; "." matches all
  println("a.b.c".split('.').toList)       // List(a, b, c) — Char overload is literal
  println("x".repeat(3))
  println("abc".padTo(6, '*'))
  println("line1\nline2".linesIterator.toList)
  println("capitalize".capitalize)
  println("42".forall(_.isDigit))
  println("hello".zipWithIndex.toList.take(2))
  println("Scala".toList)
  println("a-b-c".replace("-", "+"))
  println("a1b22c".replaceAll("[0-9]+", "#"))
  println("snake_case_word".split('_').map(_.capitalize).mkString)

  section("Equality and identity")
  val a = "hello"
  val b = "hello"                     // literals are interned by the JVM → same instance
  val c = new String("hello")
  println(a == c)                     // true — == calls equals (null-safe)
  println(a eq c)                     // false — reference identity
  println(a eq b)                     // true
  val n: String = null
  println(n == "x")                   // false, no NPE (== is null-safe)
  println("ABC".equalsIgnoreCase("abc"))
  println("apple" < "banana")         // true — StringOps provides Ordered comparisons
  println("a".compareTo("b"))         // -1

  section("Building strings efficiently")
  val sb = new StringBuilder
  for i <- 1 to 5 do sb ++= i.toString += ','
  sb.setLength(sb.length - 1)
  println(sb.result())
  println((1 to 5).mkString("[", ", ", "]"))
  println(List("a", "b").mkString)        // ab
  println(Seq.empty[Int].mkString("<", ",", ">"))  // <>

  section("Unicode & chars")
  val emoji = "héllo 👋"
  println(s"length=${emoji.length}")      // ⚠ 8 — length counts UTF-16 units, 👋 is 2
  println(s"codePoints=${emoji.codePointCount(0, emoji.length)}")  // 7
  println(emoji.reverse)                  // StringBuilder.reverse keeps surrogate pairs intact
  println("é".getBytes("UTF-8").length)   // 2
  println('é'.isLetter)

  section("Regular expressions")
  val Date = raw"(\d{4})-(\d{2})-(\d{2})".r
  "2026-11-03" match
    case Date(y, m, d) => println(s"year=$y month=$m day=$d")
    case _             => println("no match")
  // ⚠ Regex patterns must match the WHOLE string unless you use .unanchored
  "on 2026-11-03 we" match
    case Date(_, _, _)            => println("anchored match")
    case Date.unanchored(y, _, _) => println(s"unanchored found year $y")
    case _                        => ()
  println(Date.findAllIn("2020-01-01 and 2021-02-02").toList)
  println(Date.replaceAllIn("2020-01-02", m => s"${m.group(3)}/${m.group(2)}/${m.group(1)}"))
  println("""(?i)scala""".r.matches("SCALA"))   // true — inline flags
  val named = raw"(?<user>\w+)@(?<host>\w+)".r
  named.findFirstMatchIn("mail ada@lovelace now").foreach(m => println(m.group("host")))

  section("Formatting")
  println("%s is %d years".format(name, age))
  println(String.format("%08.3f", 3.14159))
  println(java.text.NumberFormat.getInstance(java.util.Locale.GERMANY).format(1234567.891))
  // Text blocks via stripMargin + interpolation
  val report =
    s"""|Name: $name
        |Age:  $age""".stripMargin
  println(report)

def section(title: String): Unit = println(s"\n=== $title ===")
