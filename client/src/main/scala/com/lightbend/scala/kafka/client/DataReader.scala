package com.lightbend.scala.kafka.client

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters._
import com.lightbend.scala.kafka.MessageListener

/**
 * A very simple Kafka consumer that reads the records that are written to Kafka by {@link DataProvider}.
 * Use this app as a sanity check if you suspect something is wrong with writing the data...
 */
object DataReader {

  def main(args: Array[String]) {

    println(s"Using kafka brokers at ${KAFKA_BROKER}")

    val listener = MessageListener(KAFKA_BROKER, DATA_TOPIC, DATA_GROUP, new RecordProcessor())
    listener.start()
  }
}
