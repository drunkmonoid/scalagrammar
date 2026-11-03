//> using scala 3.9.0
//> using platform native
// Run: scala-cli run ecosystem/platforms/02_scala_native.scala      (needs clang/LLVM)
//      scala-cli --power package ecosystem/platforms/02_scala_native.scala -o hello-native
//
// Scala Native compiles Scala ahead-of-time to a native executable via LLVM:
//   * instant start-up and low memory (no JVM), good for CLIs and small tools
//   * direct C interop: call C functions, use pointers, stack/heap allocation (Zone)
//   * most of the standard library and many pure-Scala libraries (os-lib, upickle, cats…)
// Trade-offs: slower compile/link, smaller ecosystem, no Java libraries (no JVM bytecode).

package ecosystem.platforms.native

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.libc.{stdlib, string}

// ---------- binding to C functions ----------
@extern
object libc:
  def getpid(): CInt = extern                       // from unistd.h
  def abs(x: CInt): CInt = extern                   // from stdlib.h

@main def run(): Unit =
  println("=== plain Scala, compiled to a native binary ===")
  println(List(3, 1, 2).sorted.map(_ * 10))
  println(s"  java.vm.name = ${sys.props.getOrElse("java.vm.name", "<none>")}")

  println("\n=== calling C ===")
  println(s"  getpid() = ${libc.getpid()}")
  println(s"  abs(-42) = ${libc.abs(-42)}")

  println("\n=== C strings and manual memory ===")
  Zone {                                            // memory freed when the Zone closes
    val cstr: CString = toCString("hello native")   // allocate a NUL-terminated C string
    println(s"  strlen = ${string.strlen(cstr)}")
    println(s"  back to Scala: ${fromCString(cstr).toUpperCase}")
  }

  println("\n=== pointers & arrays ===")
  val n = 5
  val arr: Ptr[CInt] = stdlib.malloc(sizeof[CInt] * n.toUSize).asInstanceOf[Ptr[CInt]]
  for i <- 0 until n do arr(i) = i * i              // pointer indexing
  val values = (0 until n).map(i => arr(i))
  stdlib.free(arr.asInstanceOf[Ptr[Byte]])
  println(s"  squares written through a pointer: $values")

  println("\n=== stack allocation ===")
  // (⚠ CLong is platform-sized — a `Size`, not a Scala Long — just like C's `long`.
  //  Use CLongLong for a guaranteed 64-bit Long.)
  val p: Ptr[CInt] = stackalloc[CInt]()
  !p = 123                                          // `!` dereferences
  !p = !p + 1                                       // (`!p += 1` does not desugar for `!`)
  println(s"  !p = ${!p}")
