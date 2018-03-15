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

  def help(message: String = "", exitCode: Int = 0): Nothing = {
    println(
      s"""
         |$message
         |
         |AkkaModelServer -h | --help | c | custom | a | actor
         |
         |-h | --help   Print this help and exit
         | c | custom   Use the custom stage implementation (default)
         | a | actor    Use the actors implementation
         |
    """.stripMargin)
    sys.exit(exitCode)
  }

  def main(args: Array[String]): Unit = {

    // You can either pick which one to run using a command-line argument, or for ease of use
    // with the IDE Run menu command, just switch which line is commented out for "case Nil => ...".
    val modelServerProcessor: ModelServerProcessor = args.toSeq match {
      case ("c"  | "custom") +: tail => CustomStageModelServerProcessor
      case ("a"  | "actor")  +: tail => ActorModelServerProcessor
      case ("-h" | "--help") +: tail => help()
      case Nil => CustomStageModelServerProcessor
//      case Nil => ActorModelServerProcessor
      case _ => help(s"Unexpected arguments: ${args.mkString(" ")}", 1)
    }

    // Data Stream
    val dataStream: Source[WineRecord, Consumer.Control] =
      Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(DATA_TOPIC))
        .map(record => DataRecord.fromByteArray(record.value)).collect { case Success(dataRecord) => dataRecord }

    // Model Stream
    val modelStream: Source[ModelWithDescriptor, Consumer.Control] =
      Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(MODELS_TOPIC))
        .map(record => ModelToServe.fromByteArray(record.value())).collect { case Success(mts) => mts }
        .map(record => ModelWithDescriptor.fromModelToServe(record)).collect { case Success(mod) => mod }

    // Using custom stage or actors implementation
    modelServerProcessor.createStreams(dataStream, modelStream)
  }
}
