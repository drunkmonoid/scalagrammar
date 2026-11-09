//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/03-oop/03_traits_linearization.scala

package exercises.oop.stackable

// Task:
// Implement stackable modifications over an abstract `IntQueue`:
//   trait IntQueue { def get(): Option[Int]; def put(x: Int): Unit }
//   class BasicIntQueue extends IntQueue
// Implement traits `Doubling` and `Incrementing` extending `IntQueue` with `abstract override def put(x: Int): Unit`.
// `Doubling` multiplies x by 2 before putting it: super.put(2 * x).
// `Incrementing` adds 1 to x before putting it: super.put(x + 1).

trait IntQueue:
  def get(): Option[Int]
  def put(x: Int): Unit

class BasicIntQueue extends IntQueue:
  private val buf = scala.collection.mutable.Queue.empty[Int]
  def get(): Option[Int] = if buf.isEmpty then None else Some(buf.dequeue())
  def put(x: Int): Unit = buf.enqueue(x)

trait Doubling extends IntQueue:
  abstract override def put(x: Int): Unit = ???

trait Incrementing extends IntQueue:
  abstract override def put(x: Int): Unit = ???

class StackableSuite extends munit.FunSuite:
  test("BasicIntQueue preserves FIFO ordering") {
    val q = new BasicIntQueue
    q.put(10)
    q.put(20)
    assertEquals(q.get(), Some(10))
    assertEquals(q.get(), Some(20))
    assertEquals(q.get(), None)
  }

  test("Incrementing with Doubling evaluates right-to-left in linearization") { // ⚠
    val q = new BasicIntQueue with Incrementing with Doubling
    q.put(3)
    assertEquals(q.get(), Some(7))
  }

  test("Doubling with Incrementing evaluates right-to-left in linearization") { // ⚠
    val q = new BasicIntQueue with Doubling with Incrementing
    q.put(3)
    assertEquals(q.get(), Some(8))
  }
