package com.lightbend.scala.kafkastreams.modelserver

import java.util.Properties

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters._
import com.lightbend.scala.kafkastreams.queriablestate.withstore.RestServiceStore
import com.lightbend.scala.kafkastreams.modelserver.customstore.CustomStoreStreamBuilder
import com.lightbend.scala.kafkastreams.modelserver.memorystore.MemoryStoreStreamBuilder
import com.lightbend.scala.kafkastreams.modelserver.standardstore.StandardStoreStreamBuilder
import com.lightbend.scala.kafkastreams.queriablestate.inmemory.RestServiceInMemory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}

import scala.util.control.NonFatal

object KafkaModelServer {

  private val port = 8888 // Port for queryable state


  def help(message: String = "", exitCode: Int = 0): Nothing = {
    println(
      s"""
         |$message
         |
         |KafkaModelServer -h | --help | m | memory | c | custom | s | standard
         |
         |-h | --help   Print this help and exit
         | m | memory   Use the in-memory state store ("naive") implementation
         | c | custom   Use the custom state store implementation (default)
         | s | standard Use the built-in ("standard") state store implementation
         |
    """.stripMargin)
    sys.exit(exitCode)
  }

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

    // You can either pick which one to run using a command-line argument, or for ease of use
    // with the IDE Run menu command, just switch which line is commented out for "case Nil => ...".
    val streams: KafkaStreams = args.toSeq match {
      case ("m"  | "memory")   +: tail =>
        setupMemoryStoreStreams(streamsConfiguration)
      case ("c"  | "custom")   +: tail =>
        setupCustomStoreStreams(streamsConfiguration)
      case ("s"  | "standard") +: tail =>
        setupStandardStoreStreams(streamsConfiguration)
      case ("-h" | "--help")   +: tail => help()
      case Nil =>
//        setupMemoryStoreStreams(streamsConfiguration)
        setupCustomStoreStreams(streamsConfiguration)
//        setupStandardStoreStreams(streamsConfiguration)
      case _ => help(s"Unexpected arguments: ${args.mkString(" ")}", 1)
    }

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
        case NonFatal(e) => println(s"During streams.close(), received: $e")
      }
    }
  }

  /*
   * Setup streams with the built-in ("standard") store.
   * Create the topology and the rest service.
   */
  private def setupStandardStoreStreams(streamsConfiguration: Properties): KafkaStreams = {
    val streams = StandardStoreStreamBuilder.createStreamsFluent(streamsConfiguration)
    // Start the Restful proxy for servicing remote access to state stores
    RestServiceStore.startRestProxy(streams, port, "standard")
    streams
  }

  /*
   * Setup streams with a custom store.
   * Create the topology and the rest service.
   */
  private def setupCustomStoreStreams(streamsConfiguration: Properties): KafkaStreams = {
    val streams = CustomStoreStreamBuilder.createStreamsFluent(streamsConfiguration)
    // Start the Restful proxy for servicing remote access to state stores
    RestServiceStore.startRestProxy(streams, port, "custom")
    streams
  }

  /*
   * Setup streams with an in-memory ("naive") store.
   * Create the topology and the rest service.
   */
  private def setupMemoryStoreStreams(streamsConfiguration: Properties): KafkaStreams = {
    val streams = MemoryStoreStreamBuilder.createStreams(streamsConfiguration)
    // Start the Restful proxy for servicing remote access to state stores
    RestServiceInMemory.startRestProxy(streams, port)
    streams
  }
}
