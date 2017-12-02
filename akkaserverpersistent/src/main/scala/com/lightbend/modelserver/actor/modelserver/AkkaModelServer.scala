package com.lightbend.modelserver.actor.modelserver

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.lightbend.configuration.kafka.ApplicationKafkaParameters._
import com.lightbend.modelServer.model.{DataRecord, ModelToServe, ModelWithDescriptor}
import com.lightbend.modelserver.actor.actors.ModelServingManager
import com.lightbend.modelserver.actor.queryablestate.QueriesAkkaHttpResource
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.concurrent.duration._

/**
 * Created by boris on 7/21/17.
 */
object AkkaModelServer {

  implicit val system = ActorSystem("ModelServing")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  println(s"Using kafka brokers at ${KAFKA_BROKER}")

  val dataConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(KAFKA_BROKER)
    .withGroupId(DATA_GROUP)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val modelConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(KAFKA_BROKER)
    .withGroupId(MODELS_GROUP)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def main(args: Array[String]): Unit = {

    val modelserver = system.actorOf(ModelServingManager.props)
    startRest(modelserver)

    Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(MODELS_TOPIC))
      .map(record => ModelToServe.fromByteArray(record.value())).filter(_.isSuccess).map(_.get)
      .map(record => ModelWithDescriptor.fromModelToServe(record)).filter(_.isSuccess).map(_.get)
      .map(modelserver ! _)

    Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(DATA_TOPIC))
      .map(record => DataRecord.fromByteArray(record.value())).filter(_.isSuccess).map(_.get)
      .map(modelserver ! _)
  }

  def startRest(modelserver: ActorRef): Unit = {

    implicit val timeout = Timeout(10 seconds)
    val host = "127.0.0.1"
    val port = 5500
    val routes: Route = QueriesAkkaHttpResource.storeRoutes(modelserver)

    Http().bindAndHandle(routes, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
        case ex =>
          println(s"Models observer could not bind to $host:$port", ex.getMessage)
      }
  }
}