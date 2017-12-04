package com.lightbend.modelServer.modelServer

import akka.stream._
import akka.stream.stage.{GraphStageLogicWithLogging, _}
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.model.{Model, ModelToServe, ModelToServeStats, ModelWithDescriptor}

import scala.collection.immutable

class ModelStage extends GraphStageWithMaterializedValue[ModelStageShape, ReadableModelStateStore] {

  val dataRecordIn = Inlet[WineRecord]("dataRecordIn")
  val modelRecordIn = Inlet[ModelWithDescriptor]("modelRecordIn")
  val scoringResultOut = Outlet[Option[Double]]("scoringOut")

  override val shape: ModelStageShape = new ModelStageShape(dataRecordIn, modelRecordIn, scoringResultOut)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, ReadableModelStateStore) = {

    val logic = new GraphStageLogicWithLogging(shape) {
      // state must be kept in the Logic instance, since it is created per stream materialization
      private var currentModel: Option[Model] = None
      private var newModel: Option[Model] = None
      var currentState: Option[ModelToServeStats] = None // exposed in materialized value
      private var newState: Option[ModelToServeStats] = None

      override def preStart(): Unit = {
        tryPull(shape.modelRecordIn)
        tryPull(shape.dataRecordIn)
      }

      setHandler(shape.modelRecordIn, new InHandler {
        override def onPush(): Unit = {
          val model = grab(shape.modelRecordIn)
          newState = Some(new ModelToServeStats(model.descriptor))
          newModel = Some(model.model)
          pull(shape.modelRecordIn)
        }
      })

      setHandler(shape.dataRecordIn, new InHandler {
        override def onPush(): Unit = {
          val record = grab(shape.dataRecordIn)
          newModel match {
            case Some(model) => {
              // close current model first
              currentModel match {
                case Some(m) => m.cleanup()
                case _ =>
              }
              // Update model
              currentModel = Some(model)
              currentState = newState
              newModel = None
            }
            case _ =>
          }
          currentModel match {
            case Some(model) => {
              val start = System.currentTimeMillis()
              val quality = model.score(record.asInstanceOf[AnyVal]).asInstanceOf[Double]
              val duration = System.currentTimeMillis() - start
              println(s"Calculated quality - $quality calculated in $duration ms")
              currentState.get.incrementUsage(duration)
              push(shape.scoringResultOut, Some(quality))
            }
            case _ => {
              println("No model available - skipping")
              push(shape.scoringResultOut, None)
            }
          }
          pull(shape.dataRecordIn)
        }
      })

      setHandler(shape.scoringResultOut, new OutHandler {
        override def onPull(): Unit = {
        }
      })
    }
    // we materialize this value so whoever runs the stream can get the current serving info
    val readableModelStateStore = new ReadableModelStateStore() {
      override def getCurrentServingInfo: ModelToServeStats = logic.currentState.getOrElse(ModelToServeStats.empty)
    }
    new Tuple2[GraphStageLogic, ReadableModelStateStore](logic, readableModelStateStore)
  }
}

class ModelStageShape(val dataRecordIn: Inlet[WineRecord], val modelRecordIn: Inlet[ModelWithDescriptor], val scoringResultOut: Outlet[Option[Double]]) extends Shape {

  override def deepCopy(): Shape = new ModelStageShape(dataRecordIn.carbonCopy(), modelRecordIn.carbonCopy(), scoringResultOut)

  override val inlets = List(dataRecordIn, modelRecordIn)
  override val outlets = List(scoringResultOut)

  // override def copyFromPorts(inlets: immutable.Seq[Inlet[_]], outlets: immutable.Seq[Outlet[_]]): Shape =
  //   new ModelStageShape(
  //     inlets(0).asInstanceOf[Inlet[WineRecord]],
  //     inlets(1).asInstanceOf[Inlet[ModelWithDescriptor]],
  //     outlets(0).asInstanceOf[Outlet[Option[Double]]]
  //   )
}
