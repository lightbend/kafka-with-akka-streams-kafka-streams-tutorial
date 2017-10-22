package com.lightbend.kafka

import java.io.{ ByteArrayOutputStream, File }
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ FileIO, Flow, Framing, Sink, Source }
import akka.util.ByteString
import com.lightbend.configuration.kafka.ApplicationKafkaParameters._
import com.lightbend.model.winerecord.WineRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import scala.concurrent.duration._

import scala.collection.immutable
import scala.concurrent.Future

/**
 * Created by boris on 5/10/17.
 *
 * Application publishing models from /data directory to Kafka
 */
object DataProvider_akka {

  implicit val system = ActorSystem("DataProvider")
  implicit val mat = ActorMaterializer()
  import system.dispatcher

  val producerSettings: ProducerSettings[Array[Byte], Array[Byte]] =
    ProducerSettings(system, new ByteArraySerializer, new ByteArraySerializer)
      .withBootstrapServers(LOCAL_KAFKA_BROKER)

  val file = "data/winequality_red.csv"
  val timeInterval = 1.second

  def main(args: Array[String]) {
    createTopic(DATA_TOPIC)

    loadRecordsIntoMemory(file).map { loadedRecords =>
      Source.cycle(() => loadedRecords.iterator)
        .statefulMapConcat(() => {
          val bos = new ByteArrayOutputStream()
          var lineCounter = 0
          def logEvery(n: Int) = {
            lineCounter += 1
            if (lineCounter % n == 0) println(s"Processed ${lineCounter} record")
          }

          wine => {
            bos.reset()
            wine.writeTo(bos)
            new ProducerRecord[Array[Byte], Array[Byte]](DATA_TOPIC, bos.toByteArray) :: Nil
          }
        })
        .via(delay)
        .runWith(Producer.plainSink(producerSettings))
    }
  }

  private def createTopic(topic: String): Unit = {
    val sender = KafkaMessageSender(LOCAL_KAFKA_BROKER, LOCAL_ZOOKEEPER_HOST)
    sender.createTopic(topic)
  }

  /** Delays each element by con*/
  private def delay[T]: Flow[T, T, NotUsed] = {
    Flow[T].mapAsync(1) { el =>
      akka.pattern.after(timeInterval, system.scheduler)(Future.successful(el))
    }
  }

  def loadRecordsIntoMemory(file: String): Future[immutable.Seq[WineRecord]] = {
    FileIO.fromPath(new File(file).toPath)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = Int.MaxValue))
      .map { line =>
        val cols = line.utf8String.split(";").map(_.trim)
        new WineRecord(
          fixedAcidity = cols(0).toDouble,
          volatileAcidity = cols(1).toDouble,
          citricAcid = cols(2).toDouble,
          residualSugar = cols(3).toDouble,
          chlorides = cols(4).toDouble,
          freeSulfurDioxide = cols(5).toDouble,
          totalSulfurDioxide = cols(6).toDouble,
          density = cols(7).toDouble,
          pH = cols(8).toDouble,
          sulphates = cols(9).toDouble,
          alcohol = cols(10).toDouble,
          dataType = "wine"
        )
      }
      .runWith(Sink.seq)
  }
}
