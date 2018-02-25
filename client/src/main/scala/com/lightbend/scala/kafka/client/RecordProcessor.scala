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
    val key = record.key()
    val value = record.value()
    println(s"Retrieved message #${RecordProcessor.count}: " +
      mkString("key", key) + ", " + mkString("value", value))
  }

  private def mkString(label: String, array: Array[Byte]) = {
    if (array == null) s"${label} = ${array}"
    else s"${label} = ${array}, size = ${array.size}, first 5 elements = ${array.take(5).mkString("[", ",", "]")}"
  }
}

object RecordProcessor {
  var count = 0L
}
