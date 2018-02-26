package com.lightbend.scala.modelServer.modelServer

import java.util.concurrent.TimeUnit

import akka.stream._
import akka.stream.stage._
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.modelServer.model.{Model, ModelToServeStats, ModelWithDescriptor, ServingResult}


class ModelStage extends GraphStageWithMaterializedValue[FlowShape[WineRecord, ServingResult], ModelStateStore] {

  val dataRecordIn = Inlet[WineRecord]("dataRecordIn")
  val scoringResultOut = Outlet[ServingResult]("scoringOut")

  override val shape: FlowShape[WineRecord, ServingResult] = FlowShape(dataRecordIn, scoringResultOut)

  class ModelLogic extends GraphStageLogicWithLogging(shape) {
    // state must be kept in the Logic instance, since it is created per stream materialization
    private var currentModel: Option[Model] = None
    private var newModel: Option[Model] = None
    var currentState: Option[ModelToServeStats] = None // exposed in materialized value
    private var newState: Option[ModelToServeStats] = None

    val setModelCB = getAsyncCallback[ModelWithDescriptor] { model =>
      println(s"Updated model: $model")
      newState = Some(ModelToServeStats(model.descriptor))
      newModel = Some(model.model)
    }

    setHandler(dataRecordIn, new InHandler {
      override def onPush(): Unit = {
        val record = grab(dataRecordIn)
        newModel.foreach { model =>
          // close current model first
          currentModel.foreach(_.cleanup())
          // Update model
          currentModel = Some(model)
          currentState = newState
          newModel = None
        }
        currentModel match {
          case Some(model) =>
            val start = System.nanoTime()
            val quality = model.score(record).asInstanceOf[Double]
            val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
            currentState = currentState.map(_.incrementUsage(duration))
            push(scoringResultOut, ServingResult(quality, duration))

          case None =>
            push(scoringResultOut, ServingResult.noModel)
        }
      }
    })

    setHandler(scoringResultOut, new OutHandler {
      override def onPull(): Unit = {
        pull(dataRecordIn)
      }
    })
  }

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, ModelStateStore) = {
    val logic = new ModelLogic

    // we materialize this value so whoever runs the stream can get the current serving info
    val modelStateStore = new ModelStateStore {
      override def getCurrentServingInfo: ModelToServeStats =
        logic.currentState.getOrElse(ModelToServeStats.empty)

      override def setModel(model: ModelWithDescriptor): Unit =
        logic.setModelCB.invoke(model)
    }
    (logic, modelStateStore)
  }
}
