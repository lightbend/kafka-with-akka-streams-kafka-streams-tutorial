package com.lightbend.scala.kafka.client

import com.lightbend.scala.kafka.RecordProcessorTrait
import org.apache.kafka.clients.consumer.ConsumerRecord

/**
 * It's generally NOT recommended to use a global, static, mutable variable (RecordProcessor.count),
 * but for our simple purposes, it's okay.
 */
class RecordProcessor extends RecordProcessorTrait[Array[Byte], Array[Byte]] {
  override def processRecord(record: ConsumerRecord[Array[Byte], Array[Byte]]): Unit = {
    RecordProcessor.count += 1
    println(s"Retrieved message #${RecordProcessor.count}, key = ${record.key}, value = ${record.value}")
  }
}

object RecordProcessor {
  var count = 0L
}
