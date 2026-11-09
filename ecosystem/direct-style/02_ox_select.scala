//> using scala 3.9.0
//> using jvm 21
//> using dep com.softwaremill.ox::core:1.1.0
// See also: ecosystem/direct-style/ox.scala
// Run: scala-cli run ecosystem/direct-style/02_ox_select.scala
//
// Direct-style channel multiplexing with Ox select.

package ecosystem.ox.select

import ox.*
import ox.channels.*
import scala.compiletime.testing.typeCheckErrors

@main def run(): Unit =
  section("Channel multiplexing with select")
  val ch1 = Channel.buffered[String](1)
  val ch2 = Channel.buffered[String](1)

  // Send a value on the second channel only; ch1 remains empty
  ch2.send("value-from-c2")

  // select waits for the first available clause among multiple channels
  val result = select(ch1.receiveClause, ch2.receiveClause) match
    case ch1.Received(v) => s"channel 1 delivered: $v"
    case ch2.Received(v) => s"channel 2 delivered: $v"

  println(result) // channel 2 delivered: value-from-c2

  section("Compile-time safety with typeCheckErrors")
  // Sending wrong type to a typed channel must fail compilation:
  doesNotCompile("""
    val ch = ox.channels.Channel.buffered[Int](1)
    ch.send("wrong type")
  """)

def section(title: String): Unit = println(s"\n=== $title ===")

inline def doesNotCompile(inline code: String): Unit =
  val msg = typeCheckErrors(code).headOption.map(_.message.linesIterator.mkString(" / "))
  println(s"  ✗ `$code`\n      → ${msg.getOrElse("UNEXPECTEDLY COMPILES")}")
