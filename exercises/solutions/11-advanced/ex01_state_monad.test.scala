//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/11-advanced/01_for_comprehensions.scala

package solutions.advanced.statemonad

// Task:
// Implement a minimal `State[S, A]` monad with `map` and `flatMap`, along with helper
// constructors `pure`, `get`, and `set`.
// Then implement `numberList[A](xs: List[A]): State[Int, List[(Int, A)]]` using a
// for-comprehension that numbers each element with an incrementing integer state.
// Example:
//   numberList(List("a", "b")).run(1) == (3, List((1, "a"), (2, "b")))

case class State[S, A](run: S => (S, A)):
  def map[B](f: A => B): State[S, B] =
    State(s =>
      val (s1, a) = run(s)
      (s1, f(a))
    )

  def flatMap[B](f: A => State[S, B]): State[S, B] =
    State(s =>
      val (s1, a) = run(s)
      f(a).run(s1)
    )

object State:
  def pure[S, A](a: A): State[S, A] = State(s => (s, a))
  def get[S]: State[S, S] = State(s => (s, s))
  def set[S](s: S): State[S, Unit] = State(_ => (s, ()))

def numberList[A](xs: List[A]): State[Int, List[(Int, A)]] =
  xs match
    case Nil => State.pure(Nil)
    case head :: tail =>
      for
        idx  <- State.get[Int]
        _    <- State.set(idx + 1)
        rest <- numberList(tail)
      yield (idx, head) :: rest

class StateMonadSuite extends munit.FunSuite:
  test("State primitives pure, get, set work with run") {
    assertEquals(State.pure[Int, String]("hello").run(0), (0, "hello"))
    assertEquals(State.get[Int].run(42), (42, 42))
    assertEquals(State.set[Int](99).run(0), (99, ()))
  }

  test("numberList numbers list elements sequentially") {
    val (finalState, numbered) = numberList(List("apple", "banana", "cherry")).run(1)
    assertEquals(finalState, 4)
    assertEquals(numbered, List((1, "apple"), (2, "banana"), (3, "cherry")))
  }

  test("numberList handles empty lists and negative starting states") { // ⚠
    val (emptyState, emptyNumbered) = numberList(List.empty[String]).run(0)
    assertEquals(emptyState, 0)
    assertEquals(emptyNumbered, Nil)

    val (negState, negNumbered) = numberList(List("single")).run(-5)
    assertEquals(negState, -4)
    assertEquals(negNumbered, List((-5, "single")))
  }
