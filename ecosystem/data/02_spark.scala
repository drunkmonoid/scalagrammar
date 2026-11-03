//> using scala 2.13.17
//> using dep org.apache.spark::spark-sql:4.3.0
//> using javaOpt --add-opens=java.base/java.lang=ALL-UNNAMED
//> using javaOpt --add-opens=java.base/java.nio=ALL-UNNAMED
//> using javaOpt --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
//> using javaOpt --add-opens=java.base/java.util=ALL-UNNAMED
//> using javaOpt --add-opens=java.base/java.lang.invoke=ALL-UNNAMED
//> using javaOpt -Xmx2g
//> using resourceDir ./spark-resources
// Run: scala-cli run ecosystem/data/02_spark.scala      (first run downloads ~300 MB)
//
// Apache Spark: distributed data processing. THIS FILE IS SCALA 2.13 — Spark does not publish
// Scala 3 artifacts (Scala 3 code can still use it via `.cross(CrossVersion.for3Use2_13)`, but
// Spark's Encoders rely on Scala 2 TypeTags, so Datasets of case classes need Scala 2).
//
// Runs in local mode (master "local[*]"): the same code runs on a cluster unchanged.
// spark-resources/log4j2.properties keeps Spark's logging at ERROR level.
//   DataFrame  = Dataset[Row]: untyped columns, optimised by Catalyst
//   Dataset[T] = typed rows (case classes), checked at compile time
//   Everything is LAZY until an action (show, collect, count, write) triggers a job.

package ecosystem.data.sparkdemo

import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.functions._

case class Sale(region: String, product: String, units: Int, price: Double)

object Main {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("scalagrammar")
      .master("local[*]")
      .config("spark.ui.enabled", "false")
      .config("spark.sql.shuffle.partitions", "4")
      .getOrCreate()
    import spark.implicits._

    try {
      val sales: Dataset[Sale] = Seq(
        Sale("EU", "laptop", 3, 1200.0), Sale("EU", "phone", 10, 650.0),
        Sale("US", "laptop", 5, 1100.0), Sale("US", "phone", 7, 700.0),
        Sale("APAC", "phone", 20, 500.0), Sale("APAC", "laptop", 1, 1300.0)
      ).toDS()

      println("=== Dataset (typed) ===")
      sales.filter(_.units >= 5).map(s => s"${s.region}:${s.product}").show(truncate = false)

      println("=== DataFrame API (untyped columns) ===")
      val revenue = sales.withColumn("revenue", $"units" * $"price")
      revenue.groupBy("region")
        .agg(sum("revenue").as("total"), countDistinct("product").as("products"))
        .orderBy(desc("total"))
        .show()

      println("=== SQL ===")
      revenue.createOrReplaceTempView("sales")
      spark.sql(
        """SELECT product, ROUND(AVG(price), 1) AS avg_price, SUM(units) AS units
          |FROM sales GROUP BY product ORDER BY units DESC""".stripMargin).show()

      println("=== Window functions ===")
      import org.apache.spark.sql.expressions.Window
      val byRegion = Window.partitionBy("region").orderBy(desc("revenue"))
      revenue.withColumn("rank", rank().over(byRegion)).filter($"rank" === 1)
        .select("region", "product", "revenue").orderBy("region").show()

      println("=== Laziness: the query plan before any job runs ===")
      revenue.filter($"revenue" > 5000).select("region").explain()

      println("=== Classic word count with the RDD API ===")
      val text = Seq("to be or not to be", "that is the question")
      spark.sparkContext.parallelize(text)
        .flatMap(_.split(" ")).map(w => (w, 1)).reduceByKey(_ + _)
        .sortBy(-_._2).take(3).foreach(println)
    } finally spark.stop()
  }
}
