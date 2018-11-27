package com.lightbend.scala.akkastream.modelserver

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters._
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.modelServer.model.{DataRecord, ModelToServe, ModelWithDescriptor}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Success, Try}

/* Created by boris on 7/21/17. */

/**
 * Entry point for the Akka model serving example.
 */
object AkkaModelServer {

  implicit val system: ActorSystem = ActorSystem("ModelServing")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val askTimeout: Timeout = Timeout(30.seconds)

  println(s"Using kafka brokers at $KAFKA_BROKER")

  val dataConsumerSettings: ConsumerSettings[Array[Byte], Array[Byte]] =
    ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(KAFKA_BROKER)
      .withGroupId(DATA_GROUP)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val modelConsumerSettings: ConsumerSettings[Array[Byte], Array[Byte]] =
    ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
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
      case _ => help(s"Unexpected arguments: ${args.mkString(" ")}", 1)
    }

    // Utility method used to test if a Try[_] succeeded or not.
    def failed[T](t: Try[T]): Boolean = t match {
      case Success(_) => false
      case _ => true
    }

    // Method that returns a println-like function, which we use in a Sink.foreach below.
    def errorPrinter(prefix: String): Any => Unit = a => println(s"$prefix: $a")

    // Data Stream
    val dataStream: Source[WineRecord, Consumer.Control] =
      Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(DATA_TOPIC))  // Consume from Kafka at most once ("fire and forget")
        .map(record => DataRecord.fromByteArray(record.value))             // Attempt to parse a new DataRecord (WineRecord from a byte array.
        .divertTo(Sink.foreach(errorPrinter("FAILED TO PARSE DATA RECORD")), failed) // If the parse failed, print an error message!
        .collect { case Success(dataRecord) => dataRecord }                // If it succeeded, extract the record from the Success wrapper.

    // Model Stream
    val modelStream: Source[ModelWithDescriptor, Consumer.Control] =
      Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(MODELS_TOPIC))
        .map(record => ModelToServe.fromByteArray(record.value()))
        .collect { case Success(mts) => mts }
        .map(record => ModelWithDescriptor.fromModelToServe(record))
        .collect { case Success(mod) => mod }

    // Exercise:
    // Like all good production code, we're ignoring errors in the `modelStream` code, when either map step fails. ;)
    // Duplicate the logic shown for `dataStream` to handle the two possible `map` places where errors could occur.
    // See the implementation of `DataRecord`, where we inject fake errors. Add the same logic to `ModelToServe` and
    // `ModelWithDescriptor`.

    // Exercise:
    // Print messages that scroll out of view is not very good for error handling. Write the errors to a special Kafka topic.

    // Using custom stage or actors implementation
    modelServerProcessor.createStreams(dataStream, modelStream)
  }
}
