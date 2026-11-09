//> using scala 3.9.0
//> using dep org.apache.kafka:kafka-streams:3.9.0
//> using dep org.apache.kafka:kafka-streams-test-utils:3.9.0
//> using dep org.slf4j:slf4j-nop:2.0.20
// See also: ecosystem/typelevel/04_fs2_streams.scala
// Run: scala-cli run ecosystem/streaming/01_kafka_streams.scala
//
// Kafka Streams topology testing without an external broker:
// 1. Constructing a stateless/stateful streaming topology with StreamsBuilder
// 2. Topology inspection via topology.describe()
// 3. Brokerless test execution using TopologyTestDriver and test topics
// 4. Record consumption and aggregation state verification

package ecosystem.streaming.kafkastreamsdemo

import org.apache.kafka.streams.{StreamsBuilder, StreamsConfig, TopologyTestDriver}
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.{Consumed, Produced}
import java.util.{Arrays, Properties}
import scala.jdk.CollectionConverters.*

@main def run(): Unit =
  section("1. Building word-count topology")
  val builder = new StreamsBuilder()
  val stringSerde = Serdes.String()
  val longSerde = Serdes.Long()

  val textLines = builder.stream("text-input", Consumed.`with`(stringSerde, stringSerde))

  val wordCounts = textLines
    .flatMapValues(line => Arrays.asList(line.toLowerCase.split("\\W+"): _*))
    .groupBy((_, word) => word)
    .count()

  wordCounts.toStream().to("word-counts-output", Produced.`with`(stringSerde, longSerde))

  val topology = builder.build()

  section("2. Topology Description")
  val description = topology.describe().toString
  println(description)
  assert(description.contains("Source: KSTREAM-SOURCE"))

  section("3. Running with TopologyTestDriver (No Broker)")
  val props = new Properties()
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-wordcount-app")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass.getName)
  props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass.getName)

  val driver = new TopologyTestDriver(topology, props)
  try
    val inputTopic = driver.createInputTopic(
      "text-input",
      stringSerde.serializer(),
      stringSerde.serializer()
    )

    val outputTopic = driver.createOutputTopic(
      "word-counts-output",
      stringSerde.deserializer(),
      longSerde.deserializer()
    )

    // Pipe input stream records
    inputTopic.pipeInput("key1", "All streams lead to Kafka")
    inputTopic.pipeInput("key2", "Hello Kafka streams")

    val outputMap = outputTopic.readKeyValuesToMap().asScala
    println("Output word counts from TopologyTestDriver:")
    outputMap.toSeq.sortBy(_._1).foreach { case (word, count) =>
      println(s"  $word: $count")
    }

    assert(outputMap("kafka") == 2L)
    assert(outputMap("streams") == 2L)
    assert(outputMap("all") == 1L)
    assert(outputMap("hello") == 1L)
    println("\nTopologyTestDriver verification completed successfully.")
  finally
    driver.close()
    println("TopologyTestDriver closed cleanly.")

def section(title: String): Unit = println(s"\n=== $title ===")
