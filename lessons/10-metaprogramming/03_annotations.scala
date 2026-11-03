//> using scala 3.9.0
// Run: scala-cli run lessons/10-metaprogramming/03_annotations.scala
//
// Annotations: metadata on definitions. Some are read by the compiler (tailrec, targetName,
// main, nowarn, ...), some by the JVM / Java tools (volatile, transient, throws, BeanProperty),
// and you can define your own (read at compile time by macros, or at runtime if they are
// Java annotations).

package meta.annotations

import scala.annotation.*
import scala.annotation.unchecked.uncheckedVariance
import scala.beans.BeanProperty

// ---------- compiler-facing annotations ----------
object Compiler:
  @tailrec def loop(n: Int, acc: Int = 0): Int = if n == 0 then acc else loop(n - 1, acc + n)

  @deprecated("use newApi", since = "2.0") def oldApi(): String = "old"
  def newApi(): String = "new"

  @nowarn("cat=deprecation") def callsOld(): String = oldApi()   // silence one warning category

  // @targetName: the JVM/Java name of a symbolic or clashing method
  @targetName("plus") def +++(other: String): String = s"+++$other"
  // overloads that erase to the same signature can be disambiguated:
  def sum(xs: List[Int]): Int = xs.sum
  @targetName("sumStrings") def sum(xs: List[String]): Int = xs.map(_.length).sum

  // @switch: assert that a match compiles to a tableswitch (warning if it can't)
  def day(n: Int): String = (n: @switch) match
    case 1 => "mon"
    case 2 => "tue"
    case _ => "other"

  // @unchecked / @uncheckedVariance: silence specific checks
  // @implicitNotFound / @implicitAmbiguous: custom error messages for missing givens
  @implicitNotFound("No Codec for ${A}. Define a `given Codec[${A}]` or derive one.")
  trait Codec[A]

  // @varargs: generate a Java-friendly varargs bridge (String...)
  @varargs def joinAll(xs: String*): String = xs.mkString(",")

  // @static: a real JVM static member in the companion class
  // @experimental: marks APIs that need -experimental to use
  // @constructorOnly: a constructor parameter that must not become a field

// ---------- JVM / Java-facing annotations ----------
class JavaBean:
  @BeanProperty var name: String = "bean"     // generates getName/setName for Java frameworks
  @transient var cache: String = ""           // skipped by Java serialization
  @volatile var flag: Boolean = false          // visibility across threads
  @throws[java.io.IOException]("on IO failure") def io(): Unit = ()

// ---------- custom annotations ----------
// Scala annotations extend StaticAnnotation (visible to the compiler / macros via TASTy).
// They are NOT available through Java reflection at runtime.
class Route(path: String) extends StaticAnnotation
@Route("/users") class UsersController

// Java annotations (with RUNTIME retention) ARE visible through reflection, e.g. those of
// JUnit, Jackson, JPA. Here we reuse a standard JDK one:
class WithJavaAnnotation:
  @Deprecated def legacy(): Unit = ()        // java.lang.Deprecated has RUNTIME retention

@main def run(): Unit =
  section("Compiler annotations in action")
  println(Compiler.loop(100))
  println(Compiler.callsOld())
  println(Compiler +++ "x")
  println(Compiler.getClass.getMethods.map(_.getName).filter(n => n == "plus" || n == "sumStrings").toList)
  println(Compiler.day(2))
  // ⚠ `import scala.annotation.*` also imported the PACKAGE scala.annotation.meta, which shadows
  //   this file's own top-level package `meta` — hence _root_ in the snippet:
  println(scala.compiletime.testing.typeCheckErrors("summon[_root_.meta.annotations.Compiler.Codec[Int]]").map(_.message))
  println(Compiler.joinAll("a", "b"))

  section("Bean properties (generated for Java frameworks; listed via reflection)")
  val beanMethods = classOf[JavaBean].getDeclaredMethods.map(_.getName)
    .filter(n => n.startsWith("get") || n.startsWith("set")).sorted.toList
  println(beanMethods)

  section("Runtime visibility")
  val legacy = classOf[WithJavaAnnotation].getMethod("legacy")
  println(s"  Java annotation visible at runtime: ${legacy.isAnnotationPresent(classOf[Deprecated])}")
  println(s"  Scala @Route visible at runtime: ${classOf[UsersController].getAnnotations.toList}")   // List()

  section("Annotations on expressions and types")
  // ⚠ `import scala.annotation.*` also imports the PACKAGE scala.annotation.unchecked, which
  //   shadows the @unchecked annotation — so it has to be written fully qualified here:
  val x = (42: @scala.unchecked)
  val nums: List[Int @uncheckedVariance] = List(1)
  println(x -> nums)

def section(title: String): Unit = println(s"\n=== $title ===")
