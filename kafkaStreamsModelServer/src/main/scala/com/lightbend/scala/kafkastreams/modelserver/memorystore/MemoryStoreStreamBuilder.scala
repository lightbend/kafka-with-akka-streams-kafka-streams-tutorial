package com.lightbend.scala.kafkastreams.modelserver.memorystore

import java.util.Properties

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters._
import org.apache.kafka.streams.{KafkaStreams, Topology}


object MemoryStoreStreamBuilder {
  // Create topology
  def createStreams(streamsConfiguration: Properties) : KafkaStreams = {

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
