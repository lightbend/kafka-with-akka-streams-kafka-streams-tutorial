package com.lightbend.modelServer.modelServer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ ConsumerSettings, Subscriptions }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import com.lightbend.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.configuration.kafka.ApplicationKafkaParameters.{ DATA_GROUP, LOCAL_KAFKA_BROKER, MODELS_GROUP }
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.ModelToServe
import com.lightbend.modelServer.kafka.EmbeddedSingleNodeKafkaCluster
import com.lightbend.modelServer.model.DataRecord
import com.lightbend.modelServer.queriablestate.QueriesAkkaHttpResource
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.concurrent.duration._
import scala.io.StdIn

/**
  * Created by boris on 7/21/17.
  */
object AkkaModelServer {

  implicit val system = ActorSystem("ModelServing")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val dataConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(LOCAL_KAFKA_BROKER)
    .withGroupId(DATA_GROUP)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  val modelConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(LOCAL_KAFKA_BROKER)
    .withGroupId(MODELS_GROUP)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  def main(args: Array[String]): Unit = {

    import ApplicationKafkaParameters._

    // Create embedded Kafka and topics
    EmbeddedSingleNodeKafkaCluster.start()
    EmbeddedSingleNodeKafkaCluster.createTopic(DATA_TOPIC)
    EmbeddedSingleNodeKafkaCluster.createTopic(MODELS_TOPIC)

    val modelStream: Source[ModelToServe, Consumer.Control] =
      Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(MODELS_TOPIC))
        .map(record => ModelToServe.fromByteArray(record.value())).filter(_.isSuccess).map(_.get)

    val dataStream: Source[WineRecord, Consumer.Control] =
      Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(DATA_TOPIC))
        .map(record => DataRecord.fromByteArray(record.value())).filter(_.isSuccess).map(_.get)

    val modelPredictions: Source[Option[Double], ReadableModelStateStore] = ModelStage.connect(modelStream, dataStream)

    val materializedReadableModelStateStore: ReadableModelStateStore = 
      modelPredictions
      .map(println(_))
      .to(Sink.ignore) // we do not read the results directly
      .run() // we run the stream, materializing the stage's StateStore
    
    startRest(materializedReadableModelStateStore)
  }

  def startRest(service : ReadableModelStateStore) : Unit = {

    implicit val timeout = Timeout(10 seconds)
    val host = "localhost"
    val port = 5000
    val routes: Route = QueriesAkkaHttpResource.storeRoutes(service)

    Http().bindAndHandle(routes, host, port) map { binding => 
      println(s"REST interface bound to ${binding.localAddress}") } recover { case ex =>
      println(s"REST interface could not bind to $host:$port", ex.getMessage)
    }
    
    StdIn.readLine()
    system.terminate()
  }
}
