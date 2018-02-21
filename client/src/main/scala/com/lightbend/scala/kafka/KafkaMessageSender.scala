package com.lightbend.scala.kafka

/**
 * Created by boris on 5/10/17.
 * Byte array sender to Kafka
 */

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.ByteArraySerializer

object MessageSender {
  private val ACKCONFIGURATION = "all" // Blocking on the full commit of the record
  private val RETRYCOUNT = "1" // Number of retries on put
  private val BATCHSIZE = "1024" // Buffers for unsent records for each partition - controlls batching
  private val LINGERTIME = "1" // Timeout for more records to arive - controlls batching
  private val BUFFERMEMORY = "1024000" // Controls the total amount of memory available to the producer for buffering. If records are sent faster than they can be transmitted to the server then this buffer space will be exhausted. When the buffer space is exhausted additional send calls will block. The threshold for time to block is determined by max.block.ms after which it throws a TimeoutException.

  def providerProperties(brokers: String, keySerializer: String, valueSerializer: String): Properties = {
    val props = new Properties
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ProducerConfig.ACKS_CONFIG, ACKCONFIGURATION)
    props.put(ProducerConfig.RETRIES_CONFIG, RETRYCOUNT)
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, BATCHSIZE)
    props.put(ProducerConfig.LINGER_MS_CONFIG, LINGERTIME)
    props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, BUFFERMEMORY)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer)
    props
  }

  def apply(brokers: String): MessageSender =
    new MessageSender(brokers)
}

class MessageSender(val brokers: String) {

  import MessageSender._
  val producer = new KafkaProducer[Array[Byte], Array[Byte]](
    providerProperties(brokers, classOf[ByteArraySerializer].getName, classOf[ByteArraySerializer].getName))

  def writeKeyValue(topic: String, key: Array[Byte], value: Array[Byte]): Unit = {
    val result = producer.send(new ProducerRecord[Array[Byte], Array[Byte]](topic, key, value)).get
    producer.flush()
  }

  /** Write a value with no key. */
  def writeValue(topic: String, value: Array[Byte]): Unit = {
    val result = producer.send(new ProducerRecord[Array[Byte], Array[Byte]](topic, value)).get
    producer.flush()
  }

  /** Write a value with no key. */
  def batchWriteValue(topic: String, batch: Seq[Array[Byte]]): Seq[RecordMetadata] = {
    val result = batch.map(value =>
      producer.send(new ProducerRecord[Array[Byte], Array[Byte]](topic, value)).get)
    producer.flush()
    result
  }

  def close(): Unit = {
    producer.close()
  }
}
