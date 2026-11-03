//> using scala 3.9.0
// Run: scala-cli run lessons/11-advanced/03_named_tuples.scala
//
// Named tuples (standard since Scala 3.7): tuples whose elements have names.
//   (name = "Ada", age = 36) : (name: String, age: Int)
// At runtime they are ordinary tuples; the names exist only at compile time.

package advanced.namedtuples

import scala.compiletime.testing.typeCheckErrors

type Person = (name: String, age: Int)

def oldest(people: List[Person]): Person = people.maxBy(_.age)

// Functions can return several named results without declaring a case class
def stats(xs: List[Int]): (min: Int, max: Int, mean: Double) =
  (min = xs.min, max = xs.max, mean = xs.sum.toDouble / xs.size)

case class City(name: String, population: Int, country: String)

@main def run(): Unit =
  section("Creating and accessing")
  val ada = (name = "Ada", age = 36)
  println(ada.name)
  println(ada.age)
  println(ada)                       // (Ada,36) — names are not part of the runtime value
  println(ada(0))                    // positional access still works
  val people: List[Person] = List(ada, (name = "Bob", age = 41))
  println(oldest(people).name)

  section("Multiple named results")
  val s = stats(List(3, 1, 4, 1, 5))
  println(s"min=${s.min} max=${s.max} mean=${s.mean}")
  val (min, max, mean) = s           // destructure positionally
  println(min + max)

  section("Pattern matching with names")
  ada match
    case (name = n, age = a) if a > 30 => println(s"  $n is over 30")
    case _                             => ()
  // Named patterns also work on CASE CLASSES, in any order, for any subset of fields:
  City("Lisbon", 545_000, "PT") match
    case City(country = "PT", name = n) => println(s"  Portuguese city $n")
    case _                              => ()

  section("Conversions")
  val plain: (String, Int) = ada                  // named → unnamed: OK (widening)
  println(plain)
  doesNotCompile("val p: advanced.namedtuples.Person = (\"x\", 1)")   // unnamed → named: needs names
  doesNotCompile("val p: advanced.namedtuples.Person = (age = 1, name = \"x\")")   // order matters
  println(ada.toTuple)                            // explicit drop of names
  val city = City("Porto", 230_000, "PT")
  val asNamed = Tuple.fromProductTyped(city)      // positional tuple from a case class
  println(asNamed)

  section("Named tuples as lightweight records in collections")
  val rows = List(
    (city = "Lisbon", temp = 21.5),
    (city = "Oslo", temp = 4.0),
    (city = "Cairo", temp = 33.1),
  )
  println(rows.filter(_.temp > 10).map(_.city))
  println(rows.sortBy(_.temp).map(r => s"${r.city}:${r.temp}").mkString(", "))
  println(rows.groupMapReduce(_.temp > 15)(_ => 1)(_ + _))

  section("Type-level view")
  // NamedTuple.Names[T] and NamedTuple.DropNames[T] expose the parts:
  summon[NamedTuple.DropNames[Person] =:= (String, Int)]
  println(scala.compiletime.constValueTuple[NamedTuple.Names[Person]])   // (name,age)

  section("Corner cases")
  // ⚠ Single-element named tuple vs assignment: `(x = 1)` is a one-element named tuple,
  //   but if `x` is a var in scope it is ambiguous with an assignment (the compiler treats
  //   it as an assignment and warns). Write `{ x = 1 }` when you mean assignment.
  val one = (count = 1)                 // no var named `count` here → a named tuple
  println(one.count)
  // ⚠ Mixing named and unnamed elements is not allowed:
  doesNotCompile("val bad = (a = 1, 2)")
  // ⚠ Names must be distinct:
  doesNotCompile("val bad = (a = 1, a = 2)")

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
