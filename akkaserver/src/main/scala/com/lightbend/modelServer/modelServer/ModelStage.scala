package com.lightbend.modelServer.modelServer

import akka.stream._
import akka.stream.scaladsl.{ GraphDSL, Source }
import akka.stream.stage.{ GraphStageLogicWithLogging, _ }
import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.model.Model
import com.lightbend.modelServer.model.PMML.PMMLModel
import com.lightbend.modelServer.model.tensorflow.TensorFlowModel
import com.lightbend.modelServer.{ ModelToServe, ModelToServeStats }

import scala.collection.immutable

object ModelStage {
  def connect(modelStream: Source[ModelToServe, _], dataStream: Source[WineRecord, _]): Source[Option[Double], ReadableModelStateStore] = {
    val model = new ModelStage()

    def keepModelMaterializedValue[M1, M2, M3](m1: M1, m2: M2, m3: M3): M3 = m3

    Source.fromGraph(
          GraphDSL.create(dataStream, modelStream, model)(keepModelMaterializedValue) {
            implicit builder => (d, m, w) =>
              import GraphDSL.Implicits._
    
              // wire together the input streams with the model stage (2 in, 1 out)
              /*
                                dataStream --> |       |
                                               | model | -> predictions
                                modelStream -> |       |
              */
    
              d ~> w.dataRecordIn
              m ~> w.modelRecordIn
              SourceShape(w.scoringResultOut)
          }
        )
  }
}

class ModelStage extends GraphStageWithMaterializedValue[ModelStageShape, ReadableModelStateStore] {

  private val factories = Map(
    ModelDescriptor.ModelType.PMML -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW -> TensorFlowModel)

  override val shape: ModelStageShape = new ModelStageShape

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, ReadableModelStateStore) = {


    val logic = new GraphStageLogicWithLogging(shape) {
      // state must be kept in the Logic instance, since it is created per stream materialization
      private var currentModel : Option[Model] = None
      private var newModel : Option[Model] = None
      var currentState : Option[ModelToServeStats] = None // exposed in materialized value
      private var newState : Option[ModelToServeStats] = None



      // TODO the pulls needed to get the stage actually pulling from the input streams
      override def preStart(): Unit = {
        tryPull(shape.modelRecordIn)
        tryPull(shape.dataRecordIn)
      }

      setHandler(shape.modelRecordIn, new InHandler {
        override def onPush(): Unit = {
          val model = grab(shape.modelRecordIn)
          println(s"New model - $model")
          newState = Some(new ModelToServeStats(model))
          newModel = factories.get(model.modelType) match{
            case Some(factory) => factory.create(model)
            case _ => None
          }
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

class ModelStageShape() extends Shape {
  var dataRecordIn = Inlet[WineRecord]("dataRecordIn")
  var modelRecordIn = Inlet[ModelToServe]("modelRecordIn")
  var scoringResultOut = Outlet[Option[Double]]("scoringOut")

  def this(dataRecordIn: Inlet[WineRecord], modelRecordIn: Inlet[ModelToServe], scoringResultOut: Outlet[Option[Double]]) {
    this()
    this.dataRecordIn = dataRecordIn
    this.modelRecordIn = modelRecordIn
    this.scoringResultOut = scoringResultOut
  }

  override def deepCopy(): Shape = new ModelStageShape(dataRecordIn.carbonCopy(), modelRecordIn.carbonCopy(), scoringResultOut)

  override def copyFromPorts(inlets: immutable.Seq[Inlet[_]], outlets: immutable.Seq[Outlet[_]]): Shape =
    new ModelStageShape(
      inlets(0).asInstanceOf[Inlet[WineRecord]],
      inlets(1).asInstanceOf[Inlet[ModelToServe]],
      outlets(0).asInstanceOf[Outlet[Option[Double]]])

  override val inlets = List(dataRecordIn, modelRecordIn)
  override val outlets = List(scoringResultOut)
}
