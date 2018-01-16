package com.lightbend.scala.custom

import java.util.Objects

import com.lightbend.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.model.winerecord.WineRecord
import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext, ProcessorSupplier}
import com.lightbend.scala.custom.store.ModelStateStore

import scala.util.Try

class DataProcessor extends AbstractProcessor[Array[Byte], Try[WineRecord]] with ProcessorSupplier[Array[Byte], Try[WineRecord]]{

  private var modelStore = null.asInstanceOf[ModelStateStore]

  import ApplicationKafkaParameters._

  override def process(key: Array[Byte], dataRecord: Try[WineRecord]): Unit = {

    modelStore.state.newModel match {
      case Some(model) => {
        // close current model first
        modelStore.state.currentModel match {
          case Some(m) => m.cleanup()
          case _ =>
        }
        // Update model
        modelStore.state.currentModel = modelStore.state.newModel
        modelStore.state.currentState = modelStore.state.newState
        modelStore.state.newModel = None
      }
      case _ =>
    }
    modelStore.state.currentModel match {
      case Some(model) => {
        val start = System.currentTimeMillis()
        val quality = model.score(dataRecord.get.asInstanceOf[AnyVal]).asInstanceOf[Double]
        val duration = System.currentTimeMillis() - start
        println(s"Calculated quality - $quality calculated in $duration ms")
        modelStore.state.currentState.get.incrementUsage(duration)
      }
      case _ => {
        println("No model available - skipping")
      }
    }
   }

  override def init(context: ProcessorContext): Unit = {
    modelStore = context.getStateStore(STORE_NAME).asInstanceOf[ModelStateStore];
    Objects.requireNonNull(modelStore, "State store can't be null")
  }

  override def get() = new DataProcessor()
}
