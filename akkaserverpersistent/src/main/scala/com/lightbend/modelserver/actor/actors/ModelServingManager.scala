package com.lightbend.modelserver.actor.actors


import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.model.ModelWithDescriptor

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import scala.concurrent.duration._

// Router actor, routing both model and data to an appropriate actor

class ModelServingManager(implicit executionContext: ExecutionContext) extends Actor {

  implicit val timeout = Timeout(500 millisecond)

  private def getModelServer(dataType: String): ActorRef = {

    var actorRef : ActorRef = null.asInstanceOf[ActorRef]

    context.actorSelection(s"./$dataType").resolveOne().onComplete{
      case Success(actor) => actorRef = actor
      case Failure(ex) => actorRef = context.actorOf(ModelServingActor.props, dataType)
    }
    actorRef
  }

  override def receive = {
    case model: ModelWithDescriptor => getModelServer(model.descriptor.dataType) ! model
    case record : WineRecord => sender() ! (getModelServer(record.dataType) ! record)
    case getState : GetState => sender() ! (getModelServer(getState.dataType) ! getState)
  }
}

object ModelServingManager{
  def props : Props = Props[ModelServingManager]
}