package com.lightbend.kafka.client

import java.io.ByteArrayOutputStream

import com.lightbend.configuration.kafka.ApplicationKafkaParameters._
import com.lightbend.kafka.KafkaMessageSender
import com.lightbend.model.winerecord.WineRecord

import scala.io.Source

/**
 * Created by boris on 5/10/17.
 *
 * Application publishing models from /data directory to Kafka
 */
object DataProvider {

  val file = "data/winequality_red.csv"
  var timeInterval = 1000 * 1 // 1 sec

  def main(args: Array[String]) {

    println(s"Using kafka brokers at ${LOCAL_KAFKA_BROKER} with zookeeper ${LOCAL_ZOOKEEPER_HOST}")
    if (args.length > 0) timeInterval = args(0).toInt
    println(s"Message delay ${timeInterval}")

    val sender = KafkaMessageSender(LOCAL_KAFKA_BROKER, LOCAL_ZOOKEEPER_HOST)
    sender.createTopic(DATA_TOPIC)
    val bos = new ByteArrayOutputStream()
    val records = getListOfRecords(file)
    var nrec = 0
    while (true) {
      records.foreach(r => {
        bos.reset()
        r.writeTo(bos)
        sender.writeValue(DATA_TOPIC, bos.toByteArray)
        nrec = nrec + 1
        if (nrec % 10 == 0)
          println(s"printed $nrec records")
        pause()
      })
    }
  }

  private def pause(): Unit = {
    try {
      Thread.sleep(timeInterval)
    } catch {
      case _: Throwable => // Ignore
    }
  }

  def getListOfRecords(file: String): Seq[WineRecord] = {

    var result = Seq.empty[WineRecord]
    val bufferedSource = Source.fromFile(file)
    for (line <- bufferedSource.getLines) {
      val cols = line.split(";").map(_.trim)
      val record = new WineRecord(
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
      result = record +: result
    }
    bufferedSource.close
    result
  }
}