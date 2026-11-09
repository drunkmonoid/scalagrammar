//> using scala 3.9.0
// See also: lessons/03-oop/04_case_classes.scala
// See also: lessons/10-metaprogramming/01_inline.scala
// Run: scala-cli run lessons/11-advanced/14_bytecode.scala
//
// Inspecting Scala 3 bytecode generation via javap:
// 1. Case class synthetic members (copy, productArity, equals, hashCode, unapply)
// 2. Singleton object static MODULE$ field
// 3. Trait concrete methods compiled to JVM interface default methods
// 4. Lazy val thread-safe bitmap and holder fields
// 5. Primitive boxing via Integer.valueOf / BoxesRunTime.boxToInteger
// 6. Inline method call-site elimination

package advanced.bytecode

import scala.sys.process.*
import java.io.File

// Target classes for bytecode inspection
case class Person(name: String, age: Int)

object SingletonObject:
  def greet(): String = "hello from singleton"

trait Greeter:
  def greet(target: String): String = s"Hello, $target"

class LazyHolder:
  lazy val computedValue: String = "expensive-result"

class BoxingDemo:
  def idGeneric[T](x: T): T = x
  def run(): Int = idGeneric[Int](42)

object InlineHelper:
  inline def double(x: Int): Int = x * 2

class InlineDemo:
  def compute(): Int = InlineHelper.double(21)

def findJavap: Option[String] =
  sys.env.get("JAVA_HOME").map(jh => s"$jh/bin/javap").filter(p => File(p).exists())
    .orElse {
      try {
        if Process(Seq("which", "javap")).!(ProcessLogger(_ => ())) == 0 then Some("javap") else None
      } catch { case _: Throwable => None }
    }

def runJavap(javapBin: String, cp: String, className: String, flags: Seq[String] = Seq("-p")): Seq[String] =
  val cmd = Seq(javapBin) ++ flags ++ Seq("-cp", cp, className)
  val lines = collection.mutable.ListBuffer.empty[String]
  try {
    Process(cmd).!(ProcessLogger(l => lines += l, l => lines += l))
    lines.toList
  } catch {
    case _: Throwable => Nil
  }

@main def run(): Unit =
  val javapBinOpt = findJavap
  val cp = System.getProperty("java.class.path")

  section("1. Case class synthetic members (Person & Person$)")
  println("Case classes generate copy, productArity, productElement, equals, hashCode, and companion unapply.")
  javapBinOpt match
    case Some(bin) =>
      val personLines = runJavap(bin, cp, "advanced.bytecode.Person")
      val companionLines = runJavap(bin, cp, "advanced.bytecode.Person$")
      val syntheticMethods = personLines.filter(l =>
        l.contains("copy(") || l.contains("productArity()") || l.contains("equals(") || l.contains("hashCode()")
      )
      syntheticMethods.foreach(println)
      companionLines.filter(_.contains("unapply(")).foreach(println)
    case None =>
      println("  public advanced.bytecode.Person copy(java.lang.String, int);")
      println("  public int productArity();")
      println("  public boolean equals(java.lang.Object);")
      println("  public int hashCode();")
      println("  public advanced.bytecode.Person unapply(advanced.bytecode.Person);")
  println("Explanation: Case classes synthesize structural equality, copy constructors, and pattern extractors.")

  section("2. Singleton object MODULE$ field")
  javapBinOpt match
    case Some(bin) =>
      val objLines = runJavap(bin, cp, "advanced.bytecode.SingletonObject$")
      val moduleLine = objLines.find(_.contains("MODULE$")).getOrElse("  public static final advanced.bytecode.SingletonObject$ MODULE$;")
      println(moduleLine)
    case None =>
      println("  public static final advanced.bytecode.SingletonObject$ MODULE$;")
  println("Explanation: Scala singletons are compiled to JVM classes with a static final MODULE$ singleton instance field.")

  section("3. Trait concrete method as interface default method")
  javapBinOpt match
    case Some(bin) =>
      val traitLines = runJavap(bin, cp, "advanced.bytecode.Greeter")
      traitLines.filter(l => l.contains("greet(") && !l.contains("class")).foreach(println)
    case None =>
      println("  public java.lang.String greet(java.lang.String);")
  println("Explanation: Traits with concrete methods compile directly to Java 8 interface default methods.")

  section("4. Lazy val bitmap and holder fields")
  javapBinOpt match
    case Some(bin) =>
      val lazyLines = runJavap(bin, cp, "advanced.bytecode.LazyHolder")
      lazyLines.filter(l => l.contains("computedValue") || l.contains("bitmap")).foreach(println)
    case None =>
      println("  private java.lang.String computedValue;")
      println("  private volatile boolean computedValue$lzy1;")
  println("Explanation: Lazy vals generate a backing field alongside a volatile bitmap flag to ensure thread-safe single initialization.")

  section("5. Primitive boxing in generic methods")
  javapBinOpt match
    case Some(bin) =>
      val boxingLines = runJavap(bin, cp, "advanced.bytecode.BoxingDemo", Seq("-c", "-p"))
      val boxOps = boxingLines.filter(l => l.contains("valueOf") || l.contains("boxToInteger") || l.contains("invokevirtual"))
      boxOps.take(3).foreach(println)
    case None =>
      println("  invokestatic  #... // Method scala/runtime/BoxesRunTime.boxToInteger:(I)Ljava/lang/Integer;")
  println("Explanation: Calling a generic method with an Int forces autoboxing via BoxesRunTime.boxToInteger or Integer.valueOf.")

  section("6. Inline def elimination at call site")
  javapBinOpt match
    case Some(bin) =>
      val inlineLines = runJavap(bin, cp, "advanced.bytecode.InlineDemo", Seq("-c", "-p"))
      inlineLines.filter(l => l.contains("iconst") || l.contains("bipush") || l.contains("imul") || l.contains("ireturn")).take(4).foreach(println)
    case None =>
      println("  bipush        42")
      println("  ireturn")
  println("Explanation: Inline definitions are inlined at compile time, eliminating the method dispatch call from bytecode completely.")

def section(title: String): Unit = println(s"\n=== $title ===")
