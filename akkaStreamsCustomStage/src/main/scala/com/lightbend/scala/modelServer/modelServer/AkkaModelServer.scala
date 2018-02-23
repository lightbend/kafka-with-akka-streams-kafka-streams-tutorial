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

  val dataConsumerSettings : ConsumerSettings[Array[Byte], Array[Byte]] = ???

  /*
     Define consumer setting with ByteArrayDeserializer and ByteArrayDeserializer
     With broker Kafka KAFKA_BROKER and kafka group DATA_GROUP
     set AUTO_OFFSET_RESET_CONFIG, "earliest"
    */

  val modelConsumerSettings : ConsumerSettings[Array[Byte], Array[Byte]] = ???

  /*
    Define consumer setting with ByteArrayDeserializer and ByteArrayDeserializer
    With broker Kafka KAFKA_BROKER and kafka group MODELS_GROUP
    set AUTO_OFFSET_RESET_CONFIG, "earliest"
   */


  def main(args: Array[String]): Unit = {

    val dataStream: Source[WineRecord, Consumer.Control] =
      Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(DATA_TOPIC))
        .map(record => DataRecord.fromByteArray(record.value))
        .collect { case Success(a) => a }

    val modelPredictions  = dataStream.viaMat(new ModelStage)(Keep.right)

        /* Add printing of the execution results. Ensure that you distinguish "No model" case
         */

    val modelStateStore: ModelStateStore =
      modelPredictions
        .to(Sink.ignore)  // we do not read the results directly
        .run()            // we run the stream, materializing the stage's StateStore

    // model stream

    /* Implement model processing. The steps here should be:
        1. Convert incoming byte array message to ModelToServe (ModelToServe.fromByteArray)
        2. Make sure you ignore records failed to marshall
        3. Convert ModelToserve to computable model (ModelWithDescriptor.fromModelToServe)
        4. Make sure you ignore models failed to convert
        5. Update the stage with a new model (modelStateStore.setModel)

     */
  }

  def startRest(service: ModelStateStore): Unit = {

    implicit val timeout = Timeout(10.seconds)
    val host = "localhost"//InetAddress.getLocalHost.getHostAddress
    val port = 5500
    val routes: Route = QueriesAkkaHttpResource.storeRoutes(service)

    Http().bindAndHandle(routes, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
      case ex =>
        println(s"Models observer could not bind to $host:$port - ${ex.getMessage}")
    }
  }
}
