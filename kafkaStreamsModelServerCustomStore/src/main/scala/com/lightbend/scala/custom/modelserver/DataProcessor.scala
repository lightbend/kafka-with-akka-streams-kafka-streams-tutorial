package com.lightbend.scala.custom.modelserver

import java.util.Objects

import org.apache.kafka.streams.processor.ProcessorContext
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.scala.custom.store.ModelStateStore
import com.lightbend.scala.modelServer.model.ServingResult
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.Transformer

import scala.util.Try

class DataProcessor extends Transformer[Array[Byte], Try[WineRecord], (Array[Byte], ServingResult)]{

  private var modelStore: ModelStateStore = null

  import ApplicationKafkaParameters._

  override def transform(key: Array[Byte], dataRecord: Try[WineRecord]) : (Array[Byte], ServingResult) = {

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
    val result = modelStore.state.currentModel match {
      case Some(model) => {
        val start = System.currentTimeMillis()
        val quality = model.score(dataRecord.get).asInstanceOf[Double]
        val duration = System.currentTimeMillis() - start
//        println(s"Calculated quality - $quality calculated in $duration ms")
        modelStore.state.currentState = modelStore.state.currentState.map(_.incrementUsage(duration))
        ServingResult(quality, duration)
      }
      case _ => {
//        println("No model available - skipping")
        ServingResult.noModel
      }
    }
    (key, result)
  }

  override def init(context: ProcessorContext): Unit = {
    modelStore = context.getStateStore(STORE_NAME).asInstanceOf[ModelStateStore];
    Objects.requireNonNull(modelStore, "State store can't be null")
  }

  override def close(): Unit = {}

  override def punctuate(timestamp: Long): (Array[Byte], ServingResult) = null
}


class DataProcessorKV extends Transformer[Array[Byte], Try[WineRecord], KeyValue[Array[Byte], ServingResult]]{

  private var modelStore: ModelStateStore = null

  import ApplicationKafkaParameters._

  override def transform(key: Array[Byte], dataRecord: Try[WineRecord]) : KeyValue[Array[Byte], ServingResult] = {
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
    val result = modelStore.state.currentModel match {
      case Some(model) => {
        val start = System.currentTimeMillis()
        val quality = model.score(dataRecord.get).asInstanceOf[Double]
        val duration = System.currentTimeMillis() - start
        //        println(s"Calculated quality - $quality calculated in $duration ms")
        modelStore.state.currentState = modelStore.state.currentState.map(_.incrementUsage(duration))
        ServingResult(quality, duration)
      }
      case _ => {
        //        println("No model available - skipping")
        ServingResult.noModel
      }
    }
    KeyValue.pair(key,result)
  }

  override def init(context: ProcessorContext): Unit = {
    modelStore = context.getStateStore(STORE_NAME).asInstanceOf[ModelStateStore];
    Objects.requireNonNull(modelStore, "State store can't be null")
  }

  override def close(): Unit = {}

  override def punctuate(timestamp: Long): KeyValue[Array[Byte], ServingResult] = null
}


