package demo.app

import demo.core.Stats

@main def run(): Unit =
  println(Stats.mean(List(3.0, 4.0, 5.0)))
  println(Stats.wordCounts(List("to be or", "not to be")))
