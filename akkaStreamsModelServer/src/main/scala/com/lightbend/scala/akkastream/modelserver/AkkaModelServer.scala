package com.lightbend.scala.akkastream.modelserver

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.Timeout

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters._
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.modelServer.model.{DataRecord, ModelToServe, ModelWithDescriptor, ServingResult}

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.concurrent.duration._
import scala.util.Success

/**
 * Created by boris on 7/21/17.
 */
object AkkaModelServer {

  implicit val system = ActorSystem("ModelServing")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val askTimeout = Timeout(30.seconds)

  println(s"Using kafka brokers at ${KAFKA_BROKER} ")

  val dataConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(KAFKA_BROKER)
    .withGroupId(DATA_GROUP)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val modelConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(KAFKA_BROKER)
    .withGroupId(MODELS_GROUP)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def main(args: Array[String]): Unit = {

    // Data Stream
    val dataStream: Source[WineRecord, Consumer.Control] =
      Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(DATA_TOPIC))
        .map(record => DataRecord.fromByteArray(record.value)).collect { case Success(dataRecord) => dataRecord }

    // Model Stream
    val modelStream: Source[ModelWithDescriptor, Consumer.Control] =
      Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(MODELS_TOPIC))
        .map(record => ModelToServe.fromByteArray(record.value())).collect { case Success(mts) => mts }
        .map(record => ModelWithDescriptor.fromModelToServe(record)).collect { case Success(mod) => mod }

    // Usingstage implementation
    ModelServerProcessor.stageModelServerProcessor(dataStream, modelStream)

    // Using actors implementation
//    ModelServerProcessor.actorModelServerProcessor(dataStream, modelStream)
  }
}
