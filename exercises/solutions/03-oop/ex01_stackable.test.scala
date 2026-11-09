//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/03-oop/03_traits_linearization.scala

package solutions.oop.stackable

trait IntQueue:
  def get(): Option[Int]
  def put(x: Int): Unit

class BasicIntQueue extends IntQueue:
  private val buf = scala.collection.mutable.Queue.empty[Int]
  def get(): Option[Int] = if buf.isEmpty then None else Some(buf.dequeue())
  def put(x: Int): Unit = buf.enqueue(x)

trait Doubling extends IntQueue:
  abstract override def put(x: Int): Unit = super.put(2 * x)

trait Incrementing extends IntQueue:
  abstract override def put(x: Int): Unit = super.put(x + 1)

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
