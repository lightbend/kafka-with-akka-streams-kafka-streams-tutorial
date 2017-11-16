package com.lightbend.naive.modelserver.scala

import java.util.Properties

import com.lightbend.configuration.kafka.ApplicationKafkaParameters
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.processor.ProcessorSupplier
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig, Topology}

object ModelServer {

  private val port = 8888 // Port for queryable state

  import ApplicationKafkaParameters._

  def main(args: Array[String]): Unit = {
    System.out.println("Using kafka brokers at " + KAFKA_BROKER)
    val streamsConfiguration = new Properties
    // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
    // against which the application is run.
    streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-model-server")
    streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, DATA_GROUP)
    // Where to find Kafka broker(s).
    streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER)
    // Provide the details of our embedded http service that we'll use to connect to this streams
    // instance and discover locations of stores.
    streamsConfiguration.put(StreamsConfig.APPLICATION_SERVER_CONFIG, "127.0.0.1:" + port)
    // Default serdes
    streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray.getClass)
    streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArray.getClass)
    // Add a topic config by prefixing with topic
    streamsConfiguration.put(StreamsConfig.topicPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG), "earliest")
    // Create topology
    val streams = createStreams(streamsConfiguration)
    // Set Stream exception handler
    streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      override def uncaughtException(t: Thread, e: Throwable): Unit = {
        System.out.println("Uncaught exception on thread " + t + " " + e.toString)
      }
    })
    // Start streams
    streams.start()
    // Start the Restful proxy for servicing remote access to state stores
    val restService = startRestProxy(streams, port)
    // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
    sys.addShutdownHook{
        try {
          streams.close()
//        restService.stop()
        } catch {
          case e: Exception =>
          // ignored
        }
    }
  }

  private def createStreams(streamsConfiguration: Properties) : KafkaStreams = { // Create topology
    val topology = new Topology
    // Data input streams
    topology.addSource("data", DATA_TOPIC)
    topology.addSource("models", MODELS_TOPIC)
    // Processors
    topology.addProcessor("data processor", new DataProcessor(), "data")
    topology.addProcessor("model processor", new ModelProcessor(), "models")
    // print topology
    println(topology.describe)
    new KafkaStreams(topology, streamsConfiguration)
  }

  private def startRestProxy(streams: KafkaStreams, port: Int) = {
//    val restService = new QueriesRestService(streams)
//    restService.start(port)
//    restService
  }

}

