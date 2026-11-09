//> using scala 3.9.0
//> using jvm 21
// See also: lessons/05-collections/07_performance_pitfalls.scala
// Run: scala-cli --power run --jmh ecosystem/benchmarks/01_collections.bench.scala -- -wi 1 -i 2 -f 1 -w 1s -r 1s
//
// JMH microbenchmarks for Scala collections:
// 1. Appending elements to List (O(n)) vs Vector (effectively O(1))
// 2. Summing an Array[Int] via Array.sum vs a primitive while loop
//
// Hand-rolled timing (e.g. System.nanoTime in lessons/05-collections/07_performance_pitfalls.scala)
// can be misleading because it suffers from:
// - Lack of JVM warm-up: JIT tiered compilation (C1 interpreter to C2 optimized machine code)
// - On-stack replacement and de-optimization during measurement
// - Dead-code elimination: the JVM eliminates computations whose results are never read
// - Shared heap and GC pauses skewing single-run metrics
// - Branch prediction pollution and cross-iteration interference
//
// JMH solves this by running isolated JVM forks (-f), measurement iterations (-i),
// warmup passes (-wi), iteration time controls (-w, -r), and using Blackhole to consume results.
// Note: This file has no @main and no def section because JMH generates the benchmark runner entry point.

package ecosystem.benchmarks

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
class CollectionsBenchmark:

  @Param(Array("100", "500"))
  var size: Int = 100

  var intArray: Array[Int] = _

  @Setup(Level.Trial)
  def setup(): Unit =
    intArray = Array.tabulate(size)(identity)

  // Benchmark 1: Appending to List vs Vector
  @Benchmark
  def appendToList(bh: Blackhole): Unit =
    var list = List.empty[Int]
    var i = 0
    while i < size do
      list = list :+ i
      i += 1
    bh.consume(list)

  @Benchmark
  def appendToVector(bh: Blackhole): Unit =
    var vec = Vector.empty[Int]
    var i = 0
    while i < size do
      vec = vec :+ i
      i += 1
    bh.consume(vec)

  // Benchmark 2: Array[Int].sum vs while loop
  @Benchmark
  def arraySumBuiltin(bh: Blackhole): Unit =
    bh.consume(intArray.sum)

  @Benchmark
  def arraySumWhileLoop(bh: Blackhole): Unit =
    var sum = 0
    var i = 0
    val arr = intArray
    val len = arr.length
    while i < len do
      sum += arr(i)
      i += 1
    bh.consume(sum)
