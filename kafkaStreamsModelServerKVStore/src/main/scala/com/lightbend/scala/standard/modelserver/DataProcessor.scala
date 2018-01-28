package com.lightbend.scala.standard.modelserver.scala

import java.util.Objects

import com.lightbend.model.winerecord.WineRecord
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.scala.standard.modelserver.scala.store.StoreState
import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext, ProcessorSupplier}
import org.apache.kafka.streams.state.KeyValueStore

import scala.util.Try

class DataProcessor extends AbstractProcessor[Array[Byte], Try[WineRecord]] with ProcessorSupplier[Array[Byte], Try[WineRecord]]{

  private var modelStore = null.asInstanceOf[KeyValueStore[Integer, StoreState]]

  import ApplicationKafkaParameters._

  override def process(key: Array[Byte], dataRecord: Try[WineRecord]): Unit = {

    var state = modelStore.get(STORE_ID)
    if (state == null) state = new StoreState

    state.newModel match {
      case Some(model) => {
        // close current model first
        state.currentModel match {
          case Some(m) => m.cleanup()
          case _ =>
        }
        // Update model
        state.currentModel = Some(model)
        state.currentState = state.newState
        state.newModel = None
      }
      case _ =>
    }
    state.currentModel match {
      case Some(model) => {
        val start = System.currentTimeMillis()
        val quality = model.score(dataRecord.get.asInstanceOf[AnyVal]).asInstanceOf[Double]
        val duration = System.currentTimeMillis() - start
        println(s"Calculated quality - $quality calculated in $duration ms")
        state.currentState.get.incrementUsage(duration)
      }
      case _ => {
        println("No model available - skipping")
      }
    }
    modelStore.put(STORE_ID, state)
  }

  override def init(context: ProcessorContext): Unit = {
    modelStore = context.getStateStore(STORE_NAME).asInstanceOf[KeyValueStore[Integer, StoreState]]
    Objects.requireNonNull(modelStore, "State store can't be null")
  }

  override def get() = new DataProcessor()
}
