package com.lightbend.modelserver.actor.actors

import akka.actor.{Actor, ActorRef, Props}
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.model.{ModelToServeStats, ModelWithDescriptor}

import scala.collection.JavaConverters._

// Router actor, routing both model and data to an appropriate actor
// Based on http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/

class ModelServingManager extends Actor {

  private def getModelServer(dataType: String): ActorRef = {
    context.child(dataType).getOrElse(context.actorOf(ModelServingActor.props(dataType), dataType))
  }

  private def last[A](list: List[A]): Option[A] = list match {
    case head :: Nil => Option(head)
    case head :: tail => last(tail)
    case _ => Option.empty
  }

  private def getInstances : Seq[String] =
    context.children.toSeq.flatMap(a => last(a.path.getElements.asScala.toList))

  override def receive = {
    case model: ModelWithDescriptor => getModelServer(model.descriptor.dataType) forward model
    case record: WineRecord => getModelServer(record.dataType) forward record
    case getState: GetState => {
      context.child(getState.dataType) match {
        case Some(actorRef) => {
          println(s"FOrwarding getState request $getState to $actorRef")
          actorRef forward getState
        }
        case _ => sender() ! ModelToServeStats()
      }
    }
    case getModels : GetModels => {
      print("Get Models request")
      sender() ! getInstances
    }
  }
}

object ModelServingManager{
  def props : Props = Props(new ModelServingManager())
}

case class GetModels()