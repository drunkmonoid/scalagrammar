//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/09-concurrency/01_threads_jmm.scala

package exercises.concurrency.counter

// Task:
// Implement a thread-safe counter class `class ThreadSafeCounter`:
//   - `def increment(): Unit`
//   - `def get: Long`
//   - `def add(delta: Long): Unit`
// The test runs 8 concurrent threads performing 10 000 increments each, verifying the final total is exactly 80 000.

class ThreadSafeCounter:
  def increment(): Unit = ???
  def get: Long = ???
  def add(delta: Long): Unit = ???

class CounterSuite extends munit.FunSuite:
  test("single thread increments and adds correctly") {
    val c = new ThreadSafeCounter
    c.increment()
    c.increment()
    c.add(10)
    assertEquals(c.get, 12L)
  }

  test("concurrent 8 threads x 10,000 increments produce exact total") { // ⚠
    val c = new ThreadSafeCounter
    val numThreads = 8
    val incrementsPerThread = 10_000
    val threads = (1 to numThreads).map { _ =>
      new Thread(() => {
        var i = 0
        while i < incrementsPerThread do
          c.increment()
          i += 1
      })
    }
    threads.foreach(_.start())
    threads.foreach(_.join())
    assertEquals(c.get, (numThreads * incrementsPerThread).toLong)
  }

  test("handles negative additions thread-safely") {
    val c = new ThreadSafeCounter
    c.add(100)
    c.add(-40)
    assertEquals(c.get, 60L)
  }
