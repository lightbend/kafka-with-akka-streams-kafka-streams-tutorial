package com.lightbend.scala.modelServer.modelServer

import scala.concurrent.duration._
import scala.util.Success
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.Timeout
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters._
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.modelServer.model.DataRecord
import com.lightbend.scala.modelServer.queriablestate.QueriesAkkaHttpResource

/**
  * Created by boris on 7/21/17.
  */
object AkkaModelServer {

  implicit val system = ActorSystem("ModelServing")   // Initialize Akka
  implicit val materializer = ActorMaterializer()     // "Materialize" our streams using Akka Actors
  implicit val executionContext = system.dispatcher   // This handles thread pools, etc.

  println(s"Using kafka brokers at ${KAFKA_BROKER} ")

  val dataConsumerSettings : ConsumerSettings[Array[Byte], Array[Byte]] =
    ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
     .withBootstrapServers(KAFKA_BROKER)
     .withGroupId(DATA_GROUP)
     .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val modelConsumerSettings : ConsumerSettings[Array[Byte], Array[Byte]] = ???  // ??? convenient "no-op"; throws an Exception.
    // Exercise: Provide implementation here.
    // Define consumer setting with ByteArrayDeserializer and ByteArrayDeserializer
    // With broker Kafka KAFKA_BROKER and kafka group MODELS_GROUP
    // set AUTO_OFFSET_RESET_CONFIG, "earliest"

  def main(args: Array[String]): Unit = {

    val dataStream: Source[WineRecord, Consumer.Control] =
      Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(DATA_TOPIC))
        .map(record => DataRecord.fromByteArray(record.value))
        .collect { case Success(a) => a }

    val modelPredictions: Source[Option[Double], ModelStateStore] =
      dataStream.viaMat(new ModelStage)(Keep.right).map { result =>
        // Exercise: Provide implementation here.
        // Add printing of the execution results. Ensure that you distinguish the "no model" case
        // 1. Match on the `result.processed` (a Boolean).
        // 2. If true, print fields in the `result` object and return the `result.result` in a `Some`.
        // 3. If false, print that no model is available and return `None`.
      }

    val modelStateStore: ModelStateStore =
      modelPredictions
        .to(Sink.ignore)  // we do not read the results directly
        // Try changing Sink.ignore to Sink.foreach(println). What gets printed. Do you understand the output?
        .run()            // we run the stream, materializing the stage's StateStore

    // model stream
    // Exercise: Provide implementation here.
    // Implement model processing (as was done in previous examples). The steps here should be:
    // 1. Convert incoming byte array message to ModelToServe (ModelToServe.fromByteArray)
    // 2. Make sure you ignore records that failed to marshal
    // 3. Convert ModelToserve to a computable model (ModelWithDescriptor.fromModelToServe)
    // 4. Make sure you ignore models that failed to convert
    // 5. Update the stage with a new model (modelStateStore.setModel)
  }

  // Serve model status: http://localhost:5500/state
  def startRest(service: ModelStateStore): Unit = {

    implicit val timeout = Timeout(10.seconds)
    val host = "localhost"  // or could use InetAddress.getLocalHost.getHostAddress
    val port = 5500
    val routes: Route = QueriesAkkaHttpResource.storeRoutes(service)

    Http().bindAndHandle(routes, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
      case ex =>
        println(s"Models observer could not bind to $host:$port - ${ex.getMessage}")
    }
  }
}
