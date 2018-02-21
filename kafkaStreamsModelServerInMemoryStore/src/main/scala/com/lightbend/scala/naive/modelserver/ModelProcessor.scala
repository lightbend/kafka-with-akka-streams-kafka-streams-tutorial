package com.lightbend.scala.naive.modelserver

import com.lightbend.scala.modelServer.model.{ModelToServe, ModelToServeStats, ModelWithDescriptor}
import com.lightbend.scala.naive.modelserver.store.StoreState
import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext, ProcessorSupplier}

import scala.util.Success

class ModelProcessor extends AbstractProcessor[Array[Byte], Array[Byte]]{

  private var modelStore: StoreState = null

  override def process (key: Array[Byte], value: Array[Byte] ): Unit = {

    ModelToServe.fromByteArray(value) match {
      case Success(descriptor) => {
        ModelWithDescriptor.fromModelToServe(descriptor) match {
          case Success(modelWithDescriptor) => {
            modelStore.newModel = Some(modelWithDescriptor.model)
            modelStore.newState = Some(ModelToServeStats(descriptor))
          }
          case _ => // ignore
        }
      }
      case _ => // ignore
    }
  }

  override def init(context: ProcessorContext): Unit = {
    modelStore = StoreState()
  }
}
