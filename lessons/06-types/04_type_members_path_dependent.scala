//> using scala 3.9.0
// Run: scala-cli run lessons/06-types/04_type_members_path_dependent.scala
//
// Abstract type members, type aliases, path-dependent types, and type projections.
// Type members are an alternative to type parameters: `trait Box { type A }` vs `trait Box[A]`.

package types.members

import scala.compiletime.testing.typeCheckErrors

// ---------- abstract type member ----------
trait Buffer:
  type Elem                       // abstract type
  def items: List[Elem]
  def add(e: Elem): Buffer { type Elem = Buffer.this.Elem }   // refinement keeps Elem known

class IntBuffer(val items: List[Int]) extends Buffer:
  type Elem = Int                 // concrete
  def add(e: Int) = IntBuffer(e :: items)

// ---------- bounded type member ----------
trait Food
class Grass extends Food:
  override def toString = "grass"
class Meat extends Food:
  override def toString = "meat"
trait Animal:
  type SuitableFood <: Food
  def eat(f: SuitableFood): String = s"${getClass.getSimpleName} eats $f"
class Cow extends Animal:
  type SuitableFood = Grass
class Lion extends Animal:
  type SuitableFood = Meat

// ---------- path-dependent types ----------
class Graph:
  class Node(val id: Int):
    var edges: List[Node] = Nil                 // a Node of THIS graph
    def connect(n: Node): Unit = edges = n :: edges
  def node(id: Int): Node = Node(id)

// ---------- type aliases ----------
type UserId = Long                               // transparent alias: UserId and Long are the same
type Pred[A] = A => Boolean                      // parameterised alias
type StringMap[V] = Map[String, V]

// ---------- a type-member–based "dependent" API ----------
trait Key:
  type Value
  def name: String
object Keys:
  object Port extends Key { type Value = Int;    val name = "port" }
  object Host extends Key { type Value = String; val name = "host" }
class Config(data: Map[String, Any]):
  def get(k: Key): k.Value = data(k.name).asInstanceOf[k.Value]   // return type depends on argument

@main def run(): Unit =
  section("Abstract type members")
  val b = IntBuffer(List(1)).add(2).add(3)
  println(b.items)
  val e: b.Elem = 3                     // b.Elem is known to be Int via the refinement
  println(e)

  section("Bounded type members prevent nonsense")
  println(Cow().eat(Grass()))
  doesNotCompile("types.members.Cow().eat(types.members.Meat())")
  // But for an *abstract* Animal, we don't know what it eats:
  val a: Animal = Cow()
  doesNotCompile("val a: types.members.Animal = types.members.Cow(); a.eat(types.members.Grass())")
  // a.SuitableFood is an unknown subtype of Food — no value of it is known.

  section("Path-dependent types: each Graph has its own Node type")
  val g1 = Graph()
  val g2 = Graph()
  val n1 = g1.node(1)
  val n2 = g1.node(2)
  n1.connect(n2)                         // same graph: OK
  val m1 = g2.node(1)
  doesNotCompile("val g1 = types.members.Graph(); val g2 = types.members.Graph(); g1.node(1).connect(g2.node(2))")
  println(n1.edges.map(_.id))
  // Type projection G#Node means "a Node of ANY Graph":
  val anyNodes: List[Graph#Node] = List(n1, m1)
  println(anyNodes.map(_.id))

  section("Dependent method types")
  val cfg = Config(Map("port" -> 8080, "host" -> "localhost"))
  val port: Int = cfg.get(Keys.Port)     // statically Int
  val host: String = cfg.get(Keys.Host)  // statically String
  println(s"$host:$port")

  section("Type aliases are transparent")
  val id: UserId = 42L
  val l: Long = id                       // no conversion — same type
  val even: Pred[Int] = _ % 2 == 0
  val sm: StringMap[Int] = Map("a" -> 1)
  println(s"$l ${even(4)} $sm")
  // ⚠ aliases give no type safety: a UserId can be passed where an OrderId (also Long) is expected.
  // Use opaque types (10_opaque_types.scala) for distinct types with zero overhead.

  section("Type parameters vs type members")
  // Type params: visible in the type (Box[Int]); good when callers choose the type.
  // Type members: hidden unless refined (Box { type A = Int }); good for "existential"
  // types and families of related types (Animal/SuitableFood), and to avoid
  // "type parameter explosion" in big hierarchies.
  trait BoxP[A] { def get: A }
  trait BoxM { type A; def get: A }
  val bp: BoxP[Int] = new BoxP[Int] { def get = 1 }
  val bm: BoxM { type A = Int } = new BoxM { type A = Int; def get = 1 }
  println(bp.get + bm.get)
  val unknown: BoxM = bm                 // existential: "a BoxM of some A"
  val x = unknown.get                    // x: unknown.A
  println(x)

  section("this.type and singleton paths")
  class Fluent:
    def step(): this.type = this
  class Fluent2 extends Fluent:
    def extra(): this.type = this
  println(Fluent2().step().extra().getClass.getSimpleName)   // keeps the subclass type

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
