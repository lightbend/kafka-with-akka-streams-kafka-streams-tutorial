package com.lightbend.custom.scala

import java.util.Objects

import com.lightbend.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.modelServer.model.{ModelToServeStats, ModelWithDescriptor}
import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext, ProcessorSupplier}
import com.lightbend.custom.scala.store.ModelStateStore

import scala.util.Try

class ModelProcessor extends AbstractProcessor[Array[Byte], Try[ModelWithDescriptor]] with ProcessorSupplier[Array[Byte], Try[ModelWithDescriptor]]{

  private var modelStore = null.asInstanceOf[ModelStateStore]

  import ApplicationKafkaParameters._
  override def process (key: Array[Byte], modelWithDescriptor: Try[ModelWithDescriptor]): Unit = {

    modelStore.state.newModel = Some(modelWithDescriptor.get.model)
    modelStore.state.newState = Some(new ModelToServeStats(modelWithDescriptor.get.descriptor))
  }

  override def init(context: ProcessorContext): Unit = {
    modelStore = context.getStateStore(STORE_NAME).asInstanceOf[ModelStateStore];
    Objects.requireNonNull(modelStore, "State store can't be null")
  }

  override def get() = new ModelProcessor()
}