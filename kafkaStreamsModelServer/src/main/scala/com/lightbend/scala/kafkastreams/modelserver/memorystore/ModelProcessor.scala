package com.lightbend.scala.kafkastreams.modelserver.memorystore

import com.lightbend.scala.kafkastreams.store.StoreState
import com.lightbend.scala.modelServer.model.{ModelToServe, ModelToServeStats, ModelWithDescriptor}
import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext}

import scala.util.Success

/**
 * Handle new model parameters; updates the current model used for scoring.
 */
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
          case _ => // error; ignore
        }
      }
      case _ => // error; ignore
    }
  }

  // Exercise:
  // Like all good production code, we're ignoring errors ;) in two places. That is, we filter to keep
  // messages where a `Success(x)` is returned true and ignore the `Failure(exception)` results.
  // With the topology API, this is harder to fix; what could you do in those `case _ =>` clauses??
  // See the implementation of `DataRecord`, where we inject fake errors. Add the same logic to `ModelToServe` and
  // `ModelWithDescriptor`.

  override def init(context: ProcessorContext): Unit = {
    modelStore = StoreState()
  }
}
