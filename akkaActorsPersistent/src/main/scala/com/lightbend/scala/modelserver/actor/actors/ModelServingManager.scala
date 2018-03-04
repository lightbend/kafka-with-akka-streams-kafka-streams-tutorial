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
    case model: ModelWithDescriptor => getModelServer(model.descriptor.dataType) forward model

    case record: WineRecord => getModelServer(record.dataType) forward record

    case getState: GetState => {
      context.child(getState.dataType) match{
        case Some(server) => server forward getState
        case _ => sender() ! ModelToServeStats.empty
      }
    }

    // Exercise: Provide implementation here.
      // If the actor getState.dataType exists -> forward a request to it.
      // Otherwise return an empty ModelToServeStats:
      // 1. Use the actor context to get the child for the state (`getState.dataType`)
      // 2. Match on the returned value, which will be an Option[ActorRef].
      // 3. If a Some(ref), forward the state to the ref
      // 4. Otherwise, send the empty `ModelToServeStats` as a message to the `sender`.

    case getModels : GetModels => sender() ! getInstances
  }
}

object ModelServingManager{
  def props : Props = Props(new ModelServingManager())
}

case class GetModels()

case class GetModelsResult(models : Seq[String])
