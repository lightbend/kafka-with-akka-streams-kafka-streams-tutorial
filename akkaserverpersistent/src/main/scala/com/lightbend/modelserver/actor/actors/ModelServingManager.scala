package com.lightbend.modelserver.actor.actors


import akka.actor.{Actor, ActorRef, Props}
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.model.ModelWithDescriptor

import scala.concurrent.ExecutionContext

// Router actor, routing both model and data to an appropriate actor

class ModelServingManager(implicit executionContext: ExecutionContext) extends Actor {

  private def getModelServer(dataType: String): ActorRef = {
    context.child(dataType).getOrElse(context.actorOf(ModelServingActor.props, dataType))
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