package com.lightbend.scala.akkastream.modelserver.actors

import akka.actor.{Actor, ActorRef, Props}
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.modelServer.model.{ModelToServeStats, ModelWithDescriptor}

/**
 * Router actor, which routes both model and data (records) to an appropriate actor
 * Based on http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/
 */
class ModelServingManager extends Actor {

  private def getModelServer(dataType: String): ActorRef = {
    // Exercise:
    // Currently, one ActorRef for each kind of data is returned. For model serving,
    // we discussed in the presentation that you might want a set of workers for better
    // scalability through parallelism. The line below,
    //   case record: WineRecord => ...
    // is where scoring is invoked. Modify this class to create a set of one or more
    // workers. Choose which one to use for a record randomly, round-robin, or whatever.
    // Add this feature without changing the public API of the class, so it's transparent
    // to users.

    // Exercise:
    // One technique used to improve scoring performance is to score each record with a set
    // of models and then pick the best result. "Best result" could mean a few things:
    // 1. The score includes a confidence level and the result with the highest confidence wins.
    // 2. To met latency requirements, at least one of the models is faster than the latency window,
    //    but less accurate. Once the latency window expires, the fast result is returned if the
    //    slower models haven't returned a result in time.
    // Modify the model management and scoring logic to implement one or both scenarios.

    context.child(dataType).getOrElse(context.actorOf(ModelServingActor.props(dataType), dataType))
  }

  private def getInstances : GetModelsResult =
    GetModelsResult(context.children.map(_.path.name).toSeq)

  override def receive: PartialFunction[Any, Unit] = {
    case model: ModelWithDescriptor => getModelServer(model.descriptor.dataType) forward model

    case record: WineRecord => getModelServer(record.dataType) forward record

    case getState: GetState => context.child(getState.dataType) match{
      case Some(server) => server forward getState
      case _ => sender() ! ModelToServeStats.empty
    }

    case _ : GetModels => sender() ! getInstances
  }
}

object ModelServingManager{
  def props : Props = Props(new ModelServingManager())
}

/** Used as an Actor message. */
case class GetModels()

/** Used as a message returned for the GetModels request. */
case class GetModelsResult(models : Seq[String])
