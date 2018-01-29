package com.lightbend.scala.kafka.client

import com.lightbend.scala.kafka.RecordProcessorTrait
import org.apache.kafka.clients.consumer.ConsumerRecord

class RecordProcessor extends RecordProcessorTrait[Array[Byte], Array[Byte]] {
  override def processRecord(record: ConsumerRecord[Array[Byte], Array[Byte]]): Unit =
    println("Get Message")
}
