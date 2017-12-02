package com.lightbend.modelserver.actor.queryablestate

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.lightbend.modelServer.model.ModelToServeStats
import com.lightbend.modelserver.actor.actors.GetState
import de.heikoseeberger.akkahttpjackson.JacksonSupport

object QueriesAkkaHttpResource extends JacksonSupport {

  def storeRoutes(modelserver: ActorRef): Route =
    get {
      path("state"/Segment) { datatype =>
        val info: ModelToServeStats = (modelserver ! GetState(datatype)).asInstanceOf[ModelToServeStats]
        complete(info)
      }
    }

}
