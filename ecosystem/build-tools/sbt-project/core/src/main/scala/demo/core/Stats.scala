package demo.core

import cats.syntax.all.*

object Stats:
  def mean(xs: List[Double]): Option[Double] =
    Option.when(xs.nonEmpty)(xs.sum / xs.size)

  /** Combines word counts using the cats Monoid for Map[String, Int]. */
  def wordCounts(lines: List[String]): Map[String, Int] =
    lines.foldMap(_.split("\\s+").filter(_.nonEmpty).map(_ -> 1).toMap)
