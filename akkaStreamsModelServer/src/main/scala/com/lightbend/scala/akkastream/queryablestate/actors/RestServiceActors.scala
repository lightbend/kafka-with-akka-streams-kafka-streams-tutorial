package com.lightbend.scala.akkastream.queryablestate.actors

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, get, onSuccess, path}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.lightbend.scala.akkastream.modelserver.actors.{GetModels, GetModelsResult, GetState}
import com.lightbend.scala.modelServer.model.ModelToServeStats
import de.heikoseeberger.akkahttpjackson.JacksonSupport

import scala.concurrent.duration._

object RestServiceActors {

  // See http://localhost:5500/models
  // Then select a model shown and try http://localhost:5500/state/<model>, e.g., http://localhost:5500/state/wine
  def startRest(modelserver: ActorRef)(implicit system: ActorSystem, materializer: ActorMaterializer): Unit = {

    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(10.seconds)
    val host = "127.0.0.1"
    val port = 5500
    val routes: Route = QueriesAkkaHttpResource.storeRoutes(modelserver)

    Http().bindAndHandle(routes, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
      case ex =>
        println(s"Models observer could not bind to $host:$port - ${ex.getMessage}")
    }
  }
}

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
