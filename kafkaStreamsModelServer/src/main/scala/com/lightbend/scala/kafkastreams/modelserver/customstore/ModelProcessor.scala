package com.lightbend.scala.kafkastreams.modelserver.customstore

import java.util.Objects

import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext}
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.scala.modelServer.model.{ModelToServeStats, ModelWithDescriptor}
import com.lightbend.scala.kafkastreams.store.store.custom.ModelStateStore

import scala.util.Try

class ModelProcessor extends AbstractProcessor[Array[Byte], Try[ModelWithDescriptor]] {

  private var modelStore: ModelStateStore = null

  import ApplicationKafkaParameters._
  override def process (key: Array[Byte], modelWithDescriptor: Try[ModelWithDescriptor]): Unit = {

    modelStore.state.newModel = Some(modelWithDescriptor.get.model)
    modelStore.state.newState = Some(ModelToServeStats(modelWithDescriptor.get.descriptor))
  }

  override def init(context: ProcessorContext): Unit = {
    modelStore = context.getStateStore(STORE_NAME).asInstanceOf[ModelStateStore];
    Objects.requireNonNull(modelStore, "State store can't be null")
  }

}