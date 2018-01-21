package com.lightbend.scala.modelserver.actor.actors

import akka.actor.{Actor, Props}
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.model.{Model, ModelToServeStats, ModelWithDescriptor}
import com.lightbend.modelserver.actor.persistence.FilePersistence

// Workhorse - doing model serving for a given data type

class ModelServingActor(dataType : String) extends Actor {

  println(s"Creating model serving actor $dataType")
  private var currentModel: Option[Model] = None
  private var newModel: Option[Model] = None
  var currentState: Option[ModelToServeStats] = None
  private var newState: Option[ModelToServeStats] = None

  override def preStart {
    val state = FilePersistence.restoreState(dataType)
    newState = state._2
    newModel = state._1
  }

  override def receive = {
    case model : ModelWithDescriptor => {
      // Update model
      newState = Some(new ModelToServeStats(model.descriptor))
      newModel = Some(model.model)
      FilePersistence.saveState(dataType, newModel.get, newState.get)
      sender() ! "Done"
    }
    case record : WineRecord => {
      // Process data
      newModel match {
        // Update model
        case Some(model) => {
          // close current model first
          currentModel match {
            case Some(m) => m.cleanup()
            case _ =>
          }
          // Update model
          currentModel = newModel
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
          sender() ! Some(quality)
        }
        case _ => {
          println("No model available - skipping")
          sender() ! None
        }
      }
    }
    case request : GetState => {
      // State query
      sender() ! currentState.getOrElse(ModelToServeStats())
    }
  }
}

object ModelServingActor{
  def props(dataType : String) : Props = Props(new ModelServingActor(dataType))
}

case class GetState(dataType : String)
