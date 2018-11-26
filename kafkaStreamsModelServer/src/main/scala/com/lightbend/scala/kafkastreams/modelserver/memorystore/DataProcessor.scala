package com.lightbend.scala.kafkastreams.modelserver.memorystore

import com.lightbend.scala.kafkastreams.store.StoreState
import com.lightbend.scala.modelServer.model.{DataRecord, ServingResult}
import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext}

import scala.util.Success

/**
 * The DataProcessor for the in-memory state store for the Kafka Streams example.
 * See also this example:
 * https://github.com/bbejeck/kafka-streams/blob/master/src/main/java/bbejeck/processor/stocks/StockSummaryProcessor.java
 */
class DataProcessor extends AbstractProcessor[Array[Byte], Array[Byte]]{

  private var modelStore: StoreState = null
  private var ctx: ProcessorContext = null

  // Exercise:
  // See the exercises described in com.lightbend.scala.kafkastreams.modelserver.customstore.DataProcessor.
  override def process(key: Array[Byte], value: Array[Byte]): Unit = {
    DataRecord.fromByteArray(value) match {
      case Success(dataRecord) => {
        modelStore.newModel match {
          case Some(model) => {
            // close current model first
            modelStore.currentModel match {
              case Some(m) => m.cleanup()
              case _ =>
            }
            // Update model
            modelStore.currentModel = Some(model)
            modelStore.currentState = modelStore.newState
            modelStore.newModel = None
          }
          case _ =>
        }
        modelStore.currentModel match {
          case Some(model) => {
            val start = System.currentTimeMillis()
            val quality = model.score(dataRecord).asInstanceOf[Double]
            val duration = System.currentTimeMillis() - start
//            println(s"Calculated quality - $quality calculated in $duration ms")
            modelStore.currentState = modelStore.currentState.map(_.incrementUsage(duration))
            ctx.forward(key, ServingResult(quality, duration))
          }
          case _ => {
//            println("No model available - skipping")
            ctx.forward(key, ServingResult.noModel)
          }
        }
        ctx.commit()
      }
      case _ => // error; ignore
        // Exercise:
        // Like all good production code, we're ignoring errors ;) here! That is, we filter to keep
        // messages where a `Success(x)` is returned true and ignore the `Failure(exception)` results.
        // With the topology API, this is harder to fix; what could you do in those `case _ =>` clauses??
        // See the implementation of `DataRecord`, where we inject fake errors. 
    }
  }

  override def init(context: ProcessorContext): Unit = {
    modelStore = StoreState()
    ctx = context
  }
}
