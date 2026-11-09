package cross

object Compat {
  implicit class IntOps(val x: Int) extends AnyVal {
    def times10: Int = x * 10
  }
}
