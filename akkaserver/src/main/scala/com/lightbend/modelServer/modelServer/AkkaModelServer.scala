package com.lightbend.modelServer.modelServer

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.{ActorMaterializer, SourceShape}
import akka.stream.scaladsl.{GraphDSL, Sink, Source}
import com.lightbend.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.configuration.kafka.ApplicationKafkaParameters.{DATA_GROUP, LOCAL_KAFKA_BROKER, MODELS_GROUP}
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.ModelToServe
import com.lightbend.modelServer.kafka.EmbeddedSingleNodeKafkaCluster
import com.lightbend.modelServer.model.DataRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer

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

    val dataStream: Source[WineRecord, Consumer.Control] =
      Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(DATA_TOPIC))
        .map(record => DataRecord.fromByteArray(record.value())).filter(_.isSuccess).map(_.get)

    val modelStream: Source[ModelToServe, Consumer.Control] =
      Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(MODELS_TOPIC))
        .map(record => ModelToServe.fromByteArray(record.value())).filter(_.isSuccess).map(_.get)

    val model = new ModelStage()

    def dropMaterializedValue[M1, M2, M3](m1: M1, m2: M2, m3: M3): NotUsed = NotUsed

    val modelPredictions = Source.fromGraph(
      GraphDSL.create(dataStream, modelStream, model)(dropMaterializedValue) {
        implicit builder => (d, m, w) =>
          import GraphDSL.Implicits._

          // wire together the input streams with the model stage (2 in, 1 out)
          /*
                            dataStream --> |       |
                                           | model | -> predictions
                            modelStream -> |       |
          */

          d ~> w.dataRecordIn
          m ~> w.modelRecordIn
          SourceShape(w.scoringResultOut)
      }
    ).runWith(Sink.ignore)
//    val done = modelPredictions.map{value => value match {
//      case Some(v) => println(v)
//      case _  => None
//    }}
  }

//  def startRest(final Source<Pair<Winerecord.WineRecord, OptionalDouble>, ReadableModelStateStore> predictions, final int port) throws Exception {
//    final QueriesAkkaHttpService httpService = new QueriesAkkaHttpService(predictions);
//    httpService.startServer("localhost", port); // TODO translated it, but does not seem to make much sense if kafka streams not used?
//  }
}