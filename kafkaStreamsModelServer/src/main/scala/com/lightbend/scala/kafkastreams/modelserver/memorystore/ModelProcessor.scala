package com.lightbend.scala.kafkastreams.modelserver.memorystore

import com.lightbend.scala.kafkastreams.store.StoreState
import com.lightbend.scala.modelServer.model.{ModelToServe, ModelToServeStats, ModelWithDescriptor}
import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext}

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
