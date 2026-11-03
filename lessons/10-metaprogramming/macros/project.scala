//> using scala 3.9.0
// Shared build settings for this multi-file lesson.
//
// Run:  scala-cli run --server=false lessons/10-metaprogramming/macros/
//
// Why --server=false: the compiler compiles Macros.scala first and SUSPENDS Main.scala until
// the macros exist. The Bloop build server (scala-cli's default) currently crashes on classes
// defined in a suspended file ("Failed to find name hashes for meta.macros.run"), so this
// lesson is compiled directly by scala-cli instead. sbt and Mill handle it fine.
