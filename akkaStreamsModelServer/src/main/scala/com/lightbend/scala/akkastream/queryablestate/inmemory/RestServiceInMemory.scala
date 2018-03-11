package com.lightbend.scala.akkastream.queryablestate.inmemory

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.lightbend.scala.akkastream.modelserver.stage.ModelStateStore
import com.lightbend.scala.modelServer.model.ModelToServeStats
import de.heikoseeberger.akkahttpjackson.JacksonSupport

import scala.concurrent.duration._

object RestServiceInMemory {

  // Serve model status: http://localhost:5500/state
  def startRest(service: ModelStateStore)(implicit system: ActorSystem, materializer: ActorMaterializer): Unit = {

    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(10.seconds)
    val host = "127.0.0.1"
    val port = 5500
    val routes = QueriesAkkaHttpResource.storeRoutes(service)

    Http().bindAndHandle(routes, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
      case ex =>
        println(s"Models observer could not bind to $host:$port - ${ex.getMessage}")
    }
  }
}

object QueriesAkkaHttpResource extends JacksonSupport {

  def storeRoutes(service: ModelStateStore): Route =
    get {
      path("state") {
        val info: ModelToServeStats = service.getCurrentServingInfo
        complete(info)
      }
    }
}