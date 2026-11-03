//> using scala 3.9.0
//> using options -experimental
// Run: scala-cli run lessons/11-advanced/07_experimental_capture_checking.scala
//
// Experimental features are compiled only with -experimental (or inside an
// @experimental scope). They may change or disappear between versions — this file is a
// snapshot of the direction Scala is heading, so expect to adapt it when upgrading.
//
// Capture checking ("capabilities"): types can say WHICH capabilities a value captures,
//   A^{io}    — a value of type A that may use capability `io`
//   A^        — captures "anything" (the universal capability cap)
//   A         — pure: captures nothing
// This lets the compiler prevent, e.g., a resource escaping the scope where it's valid.
// Enable it per file with: import language.experimental.captureChecking

package advanced.experimental

import language.experimental.captureChecking

class FileHandle:
  def write(s: String): Unit = println(s"  writing: $s")
  def close(): Unit = println("  closed")

// `f: FileHandle^` — the handle is a tracked capability.
// The result type T must not capture `f`, so the handle cannot leak out of the block.
def withFile[T](body: FileHandle^ => T): T =
  val f = FileHandle()
  try body(f) finally f.close()

@main def run(): Unit =
  println("=== Capture checking ===")
  withFile { f => f.write("hello") }          // fine: the result (Unit) doesn't capture f
  val n = withFile { f => f.write("x"); 42 }  // fine: Int is pure
  println(n)
  // ⚠ Leaking the handle through a closure is rejected at COMPILE time:
  //   val leaked = withFile { f => () => f.write("too late") }
  //   error: the result type captures f, which is not allowed to escape
  println(scala.compiletime.testing.typeCheckErrors(
    "advanced.experimental.withFile { f => () => f.write(\"too late\") }"
  ).map(_.message.linesIterator.next()))

  println("\n=== Other experimental / preview features to know about ===")
  // * Named type arguments:            f[B = Int](...)                (-language:experimental.namedTypeArguments)
  // * Erased definitions:              erased given / erased params   (language.experimental.erasedDefinitions)
  // * `into` modifier for conversions: def f(x: into Text)            (preview in recent versions)
  // * modularity / tracked params:     class C(tracked val x: T)      (language.experimental.modularity)
  // * Pure functions syntax A -> B (capture checking): functions that capture nothing
  // * Macro annotations:               @experimental MacroAnnotation
  // The authoritative list lives at https://docs.scala-lang.org/scala3/reference/experimental/
  println("see comments")
