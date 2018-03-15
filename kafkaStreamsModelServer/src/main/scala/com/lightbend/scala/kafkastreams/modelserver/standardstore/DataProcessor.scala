package com.lightbend.scala.kafkastreams.modelserver.standardstore

import java.util.Objects

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.modelServer.model.ServingResult
import com.lightbend.scala.kafkastreams.store.StoreState
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore

import scala.util.Try

class DataProcessor extends Transformer[Array[Byte], Try[WineRecord], (Array[Byte], ServingResult)]{

  private var modelStore: KeyValueStore[Integer, StoreState] = null

  import ApplicationKafkaParameters._

  override def transform(key: Array[Byte], dataRecord: Try[WineRecord]) : (Array[Byte], ServingResult) = {

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
    val result = state.currentModel match {
      case Some(model) => {
        val start = System.currentTimeMillis()
        val quality = model.score(dataRecord.get).asInstanceOf[Double]
        val duration = System.currentTimeMillis() - start
//        println(s"Calculated quality - $quality calculated in $duration ms")
        state.currentState = state.currentState.map(_.incrementUsage(duration))
        ServingResult(true, quality, duration)
      }
      case _ => {
//        println("No model available - skipping")
        ServingResult(false)
      }
    }
    modelStore.put(STORE_ID, state)
    (key, result)
  }

  override def init(context: ProcessorContext): Unit = {
    modelStore = context.getStateStore(STORE_NAME).asInstanceOf[KeyValueStore[Integer, StoreState]]
    Objects.requireNonNull(modelStore, "State store can't be null")
  }

  override def close(): Unit = {}

  override def punctuate(timestamp: Long): (Array[Byte], ServingResult) = null
}

class DataProcessorKV extends Transformer[Array[Byte], Try[WineRecord], KeyValue[Array[Byte], ServingResult]]{

  private var modelStore: KeyValueStore[Integer, StoreState] = null

  import ApplicationKafkaParameters._

  override def transform(key: Array[Byte], dataRecord: Try[WineRecord]) : KeyValue[Array[Byte], ServingResult] = {

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
    val result = state.currentModel match {
      case Some(model) => {
        val start = System.currentTimeMillis()
        val quality = model.score(dataRecord.get).asInstanceOf[Double]
        val duration = System.currentTimeMillis() - start
        //        println(s"Calculated quality - $quality calculated in $duration ms")
        state.currentState = state.currentState.map(_.incrementUsage(duration))
        ServingResult(quality, duration)
      }
      case _ => {
        //        println("No model available - skipping")
        ServingResult.noModel
      }
    }
    modelStore.put(STORE_ID, state)
    KeyValue.pair(key, result)
  }

  override def init(context: ProcessorContext): Unit = {
    modelStore = context.getStateStore(STORE_NAME).asInstanceOf[KeyValueStore[Integer, StoreState]]
    Objects.requireNonNull(modelStore, "State store can't be null")
  }

  override def close(): Unit = {}

  override def punctuate(timestamp: Long): KeyValue[Array[Byte], ServingResult] = null
}
