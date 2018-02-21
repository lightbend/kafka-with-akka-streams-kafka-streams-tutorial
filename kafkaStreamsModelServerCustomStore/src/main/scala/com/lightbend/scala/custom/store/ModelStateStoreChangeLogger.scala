package com.lightbend.scala.custom.store

import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.processor.internals.{ProcessorStateManager, RecordCollector}
import org.apache.kafka.streams.state.StateSerdes

class ModelStateStoreChangeLogger[K, V]
    (storeName: String, context: ProcessorContext, partition: Int, serialization: StateSerdes[K, V]){

  val topic = ProcessorStateManager.storeChangelogTopic(context.applicationId, storeName)
  val collector = context match {
    case rc: RecordCollector.Supplier => rc.recordCollector
    case _ => throw new RuntimeException(s"Expected a context that is a RecordCollector.Supplier, but got this: $context")
  }

  def this(storeName: String, context: ProcessorContext, serialization: StateSerdes[K, V]) {
    this(storeName, context, context.taskId.partition, serialization)
  }

  def logChange(key: K, value: V): Unit = {
    if (collector != null) {
      val keySerializer = serialization.keySerializer
      val valueSerializer = serialization.valueSerializer
      var ts = 0L
      try
        ts = context.timestamp
      catch {
        case t: Throwable =>
          ts = System.currentTimeMillis
      }
      collector.send(this.topic, key, value, this.partition, ts, keySerializer, valueSerializer)
    }
  }
}
