//> using scala 3.9.0
//> using options -experimental -language:experimental.into
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
import language.experimental.erasedDefinitions
import language.experimental.into

class FileHandle:
  def write(s: String): Unit = println(s"  writing: $s")
  def close(): Unit = println("  closed")

// `f: FileHandle^` — the handle is a tracked capability.
// The result type T must not capture `f`, so the handle cannot leak out of the block.
def withFile[T](body: FileHandle^ => T): T =
  val f = FileHandle()
  try body(f) finally f.close()

// Erased definitions: parameters that exist only for type checking and are eliminated by erasure:
def withErasedProof(erased proof: Int, label: String): String = label

// `into` modifier: marks a class/trait as an implicit conversion target without requiring
// call-site `import scala.language.implicitConversions`:
into class Text(val value: String)
given Conversion[String, Text] = Text(_)
def describeText(t: Text): String = t.value

@main def run(): Unit =
  println("=== Capture checking ===")
  withFile { f => f.write("hello") }          // fine: the result (Unit) doesn't capture f
  val n = withFile { f => f.write("x"); 42 }  // fine: Int is pure
  println(n)
  // ⚠ Leaking the handle through a closure is rejected at COMPILE time. Capture checking runs
  //   after type checking, so typeCheckErrors can't show it; this is the real compiler output:
  //
  //   val leaked = withFile { f => () => f.write("too late") }
  //   → Capability `f` outlives its scope: it leaks into outer capture set 's1 which is
  //     owned by value leaked.
  //     Found:    (f: FileHandle^) ->'s2 () ->{f} Unit
  //     Required: FileHandle^ => () ->'s1 Unit
  println("leaking closure: rejected by the compiler (see comment)")

  println("\n=== Erased definitions ===")
  println(withErasedProof(42, "erased parameter"))

  println("\n=== into modifier for conversions ===")
  // Passes a raw String where a Text is expected without call-site language import:
  println(describeText("converted string"))

  println("\n=== Other experimental / preview features to know about ===")
  // * Named type arguments:            f[B = Int](...)                (-language:experimental.namedTypeArguments)
  // * modularity / tracked params:     class C(tracked val x: T)      (language.experimental.modularity)
  // * Pure functions syntax A -> B (capture checking): functions that capture nothing
  // * Macro annotations:               @experimental MacroAnnotation
  // The authoritative list lives at https://docs.scala-lang.org/scala3/reference/experimental/
  println("see comments")
