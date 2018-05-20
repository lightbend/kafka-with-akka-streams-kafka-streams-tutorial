package com.lightbend.scala.kafkastreams.modelserver.customstore

import java.util.Objects

import org.apache.kafka.streams.processor.ProcessorContext
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.scala.modelServer.model.ServingResult
import com.lightbend.scala.kafkastreams.store.store.custom.ModelStateStore
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.Transformer

import scala.util.Try

/**
  * The DataProcessor for the custom state store for the Kafka Streams example.
  */
class DataProcessor extends Transformer[Array[Byte], Try[WineRecord], (Array[Byte], ServingResult)]{

  private var modelStore: ModelStateStore = null

  import ApplicationKafkaParameters._

  // Exercise:
  // Currently, one model for each kind of data is returned. For model serving,
  // we discussed in the presentation that you might want a set of workers for better
  // scalability through parallelism. (We discussed it in the context of the Akka Streams
  // example.) The line below,
  //   val quality = model.score(dataRecord.get).asInstanceOf[Double]
  // is where scoring is invoked. Modify this class to create a set of one or more
  // workers. Choose which one to use for a record randomly, round-robin, or whatever.
  // Add this feature without changing the public API of the class, so it's transparent
  // to users.
  // However, simply having a collection of servers won't help performance, because the current
  // invocation is synchronous. So, try adapting the Akka Actors example of model serving, with
  // a manager/router actor, so that you can invoke scoring asynchronously. How would you
  // properly integrate this approach with tbe Kafka Streams logic below?

  // Exercise:
  // One technique used to improve scoring performance is to score each record with a set
  // of models and then pick the best result. "Best result" could mean a few things:
  // 1. The score includes a confidence level and the result with the highest confidence wins.
  // 2. To met latency requirements, at least one of the models is faster than the latency window,
  //    but less accurate. Once the latency window expires, the fast result is returned if the
  //    slower models haven't returned a result in time.
  // Modify the model management and scoring logic to implement one or both scenarios. Again,
  // using Akka Actors or another concurrency library will be required.

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