//> using scala 3.9.0
// Run: scala-cli run lessons/11-advanced/04_java_interop.scala
//
// Scala runs on the JVM and calls Java directly. This lesson covers the friction points:
// collections, nulls, boxing, varargs, functional interfaces, static members, checked
// exceptions, Optional/Stream, and how Scala features look from Java.

package advanced.javainterop

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.jdk.StreamConverters.*
import scala.jdk.DurationConverters.*
import scala.concurrent.duration.*
import java.util.{ArrayList, HashMap, Optional}
import java.util.function.{Function as JFunction, Supplier}

@main def run(): Unit =
  section("Calling Java: constructors, statics, fields")
  val list = ArrayList[String]()                   // `new` optional
  list.add("b"); list.add("a")
  java.util.Collections.sort(list)                 // static method
  println(list)
  println(Integer.MAX_VALUE)                       // static field
  println(java.time.LocalDate.of(2026, 11, 3).getDayOfWeek)
  println("abc".length)                            // Java getters without () are allowed

  section("Collections: asScala / asJava (views, no copying)")
  val sbuf = list.asScala                          // mutable.Buffer view over the ArrayList
  sbuf += "c"
  println(list)                                    // [a, b, c] — same underlying list
  val jm = HashMap[String, Int]()
  jm.put("x", 1)
  println(jm.asScala.toMap)                        // copy into an immutable Map
  val jl: java.util.List[Int] = List(1, 2, 3).asJava   // read-only Java view
  try jl.add(4) catch case e: UnsupportedOperationException => println("  asJava of immutable List is read-only")
  println(Seq(1, 2).asJava.getClass.getSimpleName)

  section("Optional, Stream, functions, durations")
  val opt: Optional[String] = Optional.of("v")
  println(opt.toScala)                             // Some(v)
  println(Option.empty[Int].toJava)                // Optional.empty
  val stream = java.util.stream.IntStream.rangeClosed(1, 5).boxed()
  println(stream.toScala(List))                    // List(1, 2, 3, 4, 5)
  println(List("a", "b").asJavaSeqStream.map(_.toUpperCase).toScala(Vector))
  val jf: JFunction[String, Int] = (s: String) => s.length   // SAM conversion to a Java interface
  println(jf.apply("four"))
  locally {
    // ⚠ FunctionConverters is imported only in this block: a Seq[Int] is ALSO an Int => Int,
    //   so with both converters imported at the top, `Seq(1, 2).asJava` would be ambiguous.
    import scala.jdk.FunctionConverters.*
    // asJava picks the most specific java.util.function type: Int => Int → IntUnaryOperator
    println(((x: Int) => x + 1).asJava.getClass.getInterfaces.map(_.getSimpleName).toList)
  }
  val sup: Supplier[String] = () => "SAM conversion is automatic"
  println(sup.get)
  println(5.seconds.toJava -> java.time.Duration.ofMillis(1500).toScala)

  section("Nulls from Java")
  val props = System.getProperty("does.not.exist") // Java returns null
  println(Option(props).getOrElse("<missing>"))    // wrap at the boundary
  // With -Yexplicit-nulls the type would be String | Null and the compiler would force a check.

  section("Boxing: Java generics need reference types")
  val boxedList = ArrayList[Integer]()
  boxedList.add(1)                                 // Int → Integer automatically
  val i: Int = boxedList.get(0)                    // Integer → Int automatically
  println(i)
  val scalaInts = ArrayList[Int]()                 // also fine: erased to ArrayList<Object>
  scalaInts.add(2)
  println(scalaInts)

  section("Varargs")
  println(String.format("%s-%s", "a", "b"))        // Java varargs: just pass arguments
  val parts = Seq("x", "y")
  println(String.join("/", parts*))                // splice a Scala Seq into Java varargs
  println(java.util.Arrays.asList(1, 2, 3))

  section("Checked exceptions don't exist in Scala")
  try java.nio.file.Files.readString(java.nio.file.Path.of("/definitely/missing"))
  catch case e: java.io.IOException => println(s"  ${e.getClass.getSimpleName}")

  section("What Scala looks like from Java")
  // * object Foo          → class Foo$ with static MODULE$, plus static forwarders on class Foo
  // * def +(x)            → method named $plus
  // * val x               → private field + public method x()
  // * case class          → equals/hashCode/toString/copy/productX + companion apply/unapply
  // * trait               → interface with default methods
  // * by-name / function  → scala.Function0 / Function1 parameters
  // * Unit                → void;  Nothing → scala.runtime.Nothing$ (throws)
  // * @varargs, @throws, @BeanProperty, @static, @targetName help Java callers
  println(classOf[Point].getMethods.map(_.getName).filter(n => Set("x", "copy", "productArity", "$plus")(n)).sorted.toList)
  println(classOf[Point].getName -> Point.getClass.getName)

  section("Extending Java classes / implementing interfaces")
  class MyThread extends Thread:
    override def run(): Unit = println(s"  MyThread running")
  val t = MyThread(); t.start(); t.join()
  val cmp = new java.util.Comparator[String]:
    def compare(a: String, b: String) = a.length.compare(b.length)
  val words = ArrayList(java.util.List.of("ccc", "a", "bb"))
  words.sort(cmp)
  println(words)

case class Point(x: Int, y: Int):
  def +(o: Point): Point = Point(x + o.x, y + o.y)

def section(title: String): Unit = println(s"\n=== $title ===")
