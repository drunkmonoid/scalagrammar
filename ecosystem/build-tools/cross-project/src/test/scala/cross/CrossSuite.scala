package cross

import cross.Compat._

class CrossSuite extends munit.FunSuite {
  test("shared square works on both Scala 2 and 3") {
    assertEquals(Shared.square(4), 16)
  }

  test("compat times10 works on both Scala 2 and 3") {
    assertEquals(3.times10, 30)
  }
}
