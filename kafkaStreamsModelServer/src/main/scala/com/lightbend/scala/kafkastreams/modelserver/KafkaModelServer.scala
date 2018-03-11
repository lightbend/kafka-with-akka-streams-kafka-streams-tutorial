package com.lightbend.scala.kafkastreams.modelserver

import java.util.Properties

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters._
import com.lightbend.scala.kafkastreams.queriablestate.withstore.RestServiceStore
import com.lightbend.scala.kafkastreams.modelserver.customstore.CustomStoreStreamBuilder
import com.lightbend.scala.kafkastreams.modelserver.memorystore.MemoryStoreStreamBuilder
import com.lightbend.scala.kafkastreams.modelserver.standardstore.StandardStoreStreamBuilder
import com.lightbend.scala.kafkastreams.queriablestate.inmemory.RestServiceInMemory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig

object KafkaModelServer {

  private val port = 8888 // Port for queryable state


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
/*
    // memory Store
    // Create topology
    val streams = MemoryStoreStreamBuilder.createStreams(streamsConfiguration)
    // Start the Restful proxy for servicing remote access to state stores
    RestServiceInMemory.startRestProxy(streams, port)
*/

    // Custom Store
    // Create topology
    val streams = CustomStoreStreamBuilder.createStreamsFluent(streamsConfiguration)
    // Start the Restful proxy for servicing remote access to state stores
    RestServiceStore.startRestProxy(streams, port, "custom")

/*
    // Standard Store
    // Create topology
    val streams = StandardStoreStreamBuilder.createStreamsFluent(streamsConfiguration)
    // Start the Restful proxy for servicing remote access to state stores
    RestServiceStore.startRestProxy(streams, port, "standard")
*/
    streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      override def uncaughtException(t: Thread, e: Throwable): Unit = {
        System.out.println("Uncaught exception on thread " + t + " " + e.toString)
      }
    })
    // Start streams
    streams.start()
    // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
    sys.addShutdownHook{
      try {
        streams.close()
      } catch {
        case e: Exception =>
        // ignored
      }
    }
  }
}
