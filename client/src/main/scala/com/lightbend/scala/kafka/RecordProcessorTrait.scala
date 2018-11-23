package com.lightbend.scala.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord

/**
 * Abstraction for logic used to process a record in Kafka.
 */
trait RecordProcessorTrait[K, V] {

  def processRecord(record: ConsumerRecord[K, V]): Unit
}
