package com.lightbend.kafka

import java.io.ByteArrayOutputStream

import com.lightbend.model.winerecord.WineRecord

import scala.io.Source

/**
  * Created by boris on 5/10/17.
  *
  * Application publishing models from /data directory to Kafka
  */
object DataProvider {

  val file = "data/winequality_red.csv"
  val timeInterval = 1000 * 1        // 1 sec

  def main(args: Array[String]) {
    val sender = KafkaMessageSender(ApplicationKafkaParameters.LOCAL_KAFKA_BROKER, ApplicationKafkaParameters.LOCAL_ZOOKEEPER_HOST)
    sender.createTopic(ApplicationKafkaParameters.DATA_TOPIC)
    val bos = new ByteArrayOutputStream()
    val records  = getListOfRecords(file)
    while (true) {
      var lineCounter = 0
      records.foreach(r => {
        bos.reset()
        r.writeTo(bos)
        lineCounter = lineCounter + 1
        if(lineCounter % 50 == 0)
          println(s"Processed $lineCounter record")
        sender.writeValue(ApplicationKafkaParameters.DATA_TOPIC, bos.toByteArray)
        pause()
      })
      pause()
    }
  }

  private def pause() : Unit = {
    try{
      Thread.sleep(timeInterval)
    }
    catch {
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