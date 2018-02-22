package com.lightbend.scala.custom.modelserver

import java.util.Objects

import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext, ProcessorSupplier}
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.scala.modelServer.model.{ModelToServeStats, ModelWithDescriptor}
import com.lightbend.scala.custom.store.ModelStateStore

import scala.util.Try

class ModelProcessor extends AbstractProcessor[Array[Byte], Try[ModelWithDescriptor]] with ProcessorSupplier[Array[Byte], Try[ModelWithDescriptor]]{

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

  override def get() = new ModelProcessor()
}
