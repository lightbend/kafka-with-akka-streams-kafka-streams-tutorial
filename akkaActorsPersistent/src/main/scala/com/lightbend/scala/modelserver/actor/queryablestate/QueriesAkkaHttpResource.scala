package com.lightbend.scala.modelserver.actor.queryablestate

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.util.Timeout

import scala.concurrent.duration._
import com.lightbend.scala.modelServer.model.ModelToServeStats
import com.lightbend.scala.modelserver.actor.actors.{GetModels, GetModelsResult, GetState}
import de.heikoseeberger.akkahttpjackson.JacksonSupport
import akka.pattern.ask

object QueriesAkkaHttpResource extends JacksonSupport {

  implicit val askTimeout = Timeout(30.seconds)

  def storeRoutes(modelserver: ActorRef): Route =
    get {
      path("state"/Segment) { datatype =>
        onSuccess(modelserver ? GetState(datatype)) {
          case info: ModelToServeStats =>
            complete(info)
        }
      } ~
        path("models") {
          onSuccess(modelserver ? GetModels()) {
            case models: GetModelsResult =>
              complete(models)
          }
        }
    }
}
