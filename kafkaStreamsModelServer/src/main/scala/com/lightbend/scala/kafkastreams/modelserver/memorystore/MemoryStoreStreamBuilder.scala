package com.lightbend.scala.kafkastreams.modelserver.memorystore

import java.util.Properties

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters._
import org.apache.kafka.streams.{KafkaStreams, Topology}


object MemoryStoreStreamBuilder {
  // Create topology
  def createStreams(streamsConfiguration: Properties) : KafkaStreams = {

    // Exercise:
    // We use a `PrintProcessor` to print the result, but not do anything else with it.
    // In particular, we might want to write the results to a new Kafka topic.
    // 1. Modify the "client" to create a new output topic.
    // 2. Modify KafkaModelServer to add the configuration for the new topic.
    // 3. Add a final step that writes the results to the new topic.
    //    Consult the Kafka Streams Topology documentation for details.

    val topology = new Topology
    // Data input streams
    topology.addSource("data", DATA_TOPIC)
    topology.addSource("models", MODELS_TOPIC)
    // Processors
    topology.addProcessor("data processor", () => new DataProcessor(), "data")
    topology.addProcessor("printer", () => new PrintProcessor(), "data processor")
    topology.addProcessor("model processor", () => new ModelProcessor(), "models")
    // print topology
    println(topology.describe)
    new KafkaStreams(topology, streamsConfiguration)
  }
}
