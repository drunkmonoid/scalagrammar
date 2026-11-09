//> using scala 3.9.0
//> using dep co.fs2::fs2-core:3.14.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: ecosystem/typelevel/04_fs2_streams.scala

package solutions.typelevel.fs2chunks

// Task:
// Implement `def sumChunks(chunkSize: Int): fs2.Pipe[fs2.Pure, Int, Int]`.
// Group the input stream into chunks of size `chunkSize` and emit the sum of each chunk.
// If the total number of elements is not a multiple of `chunkSize`, the final chunk will be
// partial and its sum must still be emitted.
// Example:
//   fs2.Stream(1, 2, 3, 4, 5).through(sumChunks(2)).toList == List(3, 7, 5)

def sumChunks(chunkSize: Int): fs2.Pipe[fs2.Pure, Int, Int] =
  s => s.chunkN(chunkSize).map(_.foldLeft(0)(_ + _))

class Fs2ChunksSuite extends munit.FunSuite:
  test("sums chunks of exact multiple size") {
    val res = fs2.Stream(1, 2, 3, 4, 5, 6).through(sumChunks(3)).toList
    assertEquals(res, List(6, 15))
  }

  test("sums chunks with a partial final chunk") { // ⚠
    val res = fs2.Stream(1, 2, 3, 4, 5).through(sumChunks(2)).toList
    assertEquals(res, List(3, 7, 5))
  }

  test("handles empty stream and single-element stream") {
    val emptyRes = fs2.Stream.empty.covaryOutput[Int].through(sumChunks(4)).toList
    assertEquals(emptyRes, Nil)
    val singleRes = fs2.Stream(42).through(sumChunks(5)).toList
    assertEquals(singleRes, List(42))
  }
