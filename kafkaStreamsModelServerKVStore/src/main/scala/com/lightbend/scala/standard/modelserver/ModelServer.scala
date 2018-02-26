package com.lightbend.scala.standard.modelserver.scala

import java.util.Properties

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig}
import org.apache.kafka.streams.kstream.{KStream, Predicate, ValueMapper}
import org.apache.kafka.streams.state.Stores

import scala.concurrent.duration._
import java.util.HashMap

import com.lightbend.model.winerecord.WineRecord
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.scala.standard.modelserver.scala.queriablestate.QueriesResource
import com.lightbend.scala.standard.modelserver.scala.store.ModelStateSerde
import com.lightbend.scala.modelServer.model.{ModelToServe, ModelWithDescriptor, ServingResult}

import scala.util.Try


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
//    streamsConfiguration.put(StreamsConfig.topicPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG), "earliest")
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
    startRestProxy(streams, port)
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

  private def createStreams(streamsConfiguration: Properties) : KafkaStreams = { // Create topology

    // Store definition
    val logConfig = new HashMap[String, String]
    val storeSupplier = Stores.inMemoryKeyValueStore(STORE_NAME)
    val storeBuilder = Stores.keyValueStoreBuilder(storeSupplier, Serdes.Integer, new ModelStateSerde).withLoggingEnabled(logConfig)

    // Create Stream builder
    val builder = new StreamsBuilder
    // Data input streams
    val data : KStream[Array[Byte], Array[Byte]] = builder.stream(DATA_TOPIC)
    val models : KStream[Array[Byte], Array[Byte]] = builder.stream(MODELS_TOPIC)

    // DataStore
    builder.addStateStore(storeBuilder)

    // Data Processor
    data
      .mapValues[Try[WineRecord]](new DataValueMapper().asInstanceOf[ValueMapper[Array[Byte], Try[WineRecord]]])
      .filter(new DataValueFilter().asInstanceOf[Predicate[Array[Byte], Try[WineRecord]]])
      .transform(() => new DataProcessorKV, STORE_NAME)
      .mapValues[ServingResult](new ResultPrinter())
    // Models Processor
    models
      .mapValues[Try[ModelToServe]](new ModelValueMapper().asInstanceOf[ValueMapper[Array[Byte],Try[ModelToServe]]])
      .filter(new ModelValueFilter().asInstanceOf[Predicate[Array[Byte], Try[ModelToServe]]])
      .mapValues[Try[ModelWithDescriptor]](new ModelDescriptorMapper().asInstanceOf[ValueMapper[Try[ModelToServe],Try[ModelWithDescriptor]]])
      .filter((new ModelDescriptorFilter().asInstanceOf[Predicate[Array[Byte], Try[ModelWithDescriptor]]]))
      .process(() => new ModelProcessor, STORE_NAME)

    // Create and build topology
    val topology = builder.build
    println(topology.describe)

    return new KafkaStreams(topology, streamsConfiguration)

  }

  // Surf to http://localhost:8888/state/instances for the list of currently deployed instances.
  // Then surf to http://localhost:8888/state/value for the current state of execution for a given model.
  private def startRestProxy(streams: KafkaStreams, port: Int) = {

    implicit val system = ActorSystem("ModelServing")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(10.seconds)
    val host = "127.0.0.1"
    val port = 8888
    val routes: Route = QueriesResource.storeRoutes(streams, port)

    Http().bindAndHandle(routes, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
      case ex =>
        println(s"Models observer could not bind to $host:$port - ${ex.getMessage}")
    }
  }

}
