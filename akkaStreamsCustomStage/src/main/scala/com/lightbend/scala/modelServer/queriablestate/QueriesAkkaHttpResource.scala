package com.lightbend.scala.modelServer.queriablestate

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.lightbend.scala.modelServer.model.ModelToServeStats
import com.lightbend.scala.modelServer.modelServer.ModelStateStore
import de.heikoseeberger.akkahttpjackson.JacksonSupport

object QueriesAkkaHttpResource extends JacksonSupport {

  def storeRoutes(predictions: ModelStateStore): Route =
    get {
      path("state") {
        val info: ModelToServeStats = predictions.getCurrentServingInfo
        complete(info)
      }
    }
}
