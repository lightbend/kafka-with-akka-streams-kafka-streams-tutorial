package com.lightbend.scala.modelserver.actor.actors

import akka.actor.{Actor, ActorRef, Props}
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.modelServer.model.{ModelToServeStats, ModelWithDescriptor}


// Router actor, routing both model and data to an appropriate actor
// Based on http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/

class ModelServingManager extends Actor {

  private def getModelServer(dataType: String): ActorRef = {
    context.child(dataType).getOrElse(context.actorOf(ModelServingActor.props(dataType), dataType))
  }

  private def getInstances : GetModelsResult =
    GetModelsResult(context.children.map(_.path.name).toSeq)

  override def receive = {
    case model: ModelWithDescriptor =>
    /* Provide implementation here.
       Forward request to the appropriate instance (record.dataType) of the model server
     */

    case record: WineRecord => getModelServer(record.dataType) forward record
    case getState: GetState =>
      /* Provide an implementation here
         If the actor getState.dataType exists -> forward a request to it
         otherwize return an empty ModelToServeStats
       */

    case getModels : GetModels => sender() ! getInstances
  }
}

object ModelServingManager{
  def props : Props = Props(new ModelServingManager())
}

case class GetModels()

case class GetModelsResult(models : Seq[String])
