package com.lightbend.modelserver.actor.actors

import akka.actor.{Actor, Props}
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.model.{Model, ModelToServeStats, ModelWithDescriptor}

// Workhorse - doing model serving for a given data type

class ModelServingActor extends Actor {

  private var currentModel: Option[Model] = None
  private var newModel: Option[Model] = None
  var currentState: Option[ModelToServeStats] = None // exposed in materialized value
  private var newState: Option[ModelToServeStats] = None

  override def receive = {
    case model : ModelWithDescriptor => {
      // Update model
      newState = Some(new ModelToServeStats(model.descriptor))
      newModel = Some(model.model)
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
          sender() ! quality
        }
        case _ => {
          println("No model available - skipping")
          sender() ! null
        }
      }
    }
    case GetState => {
      // State query
      sender() ! currentState.getOrElse(ModelToServeStats())
    }
  }
}

object ModelServingActor{
  def props : Props = Props[ModelServingActor]
}

case class GetState(dataType : String)