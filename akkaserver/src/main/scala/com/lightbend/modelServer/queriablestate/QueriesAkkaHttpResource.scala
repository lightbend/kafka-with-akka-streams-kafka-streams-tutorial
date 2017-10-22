package com.lightbend.modelServer.queriablestate

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.lightbend.modelServer.ModelToServeStats
import com.lightbend.modelServer.modelServer.ReadableModelStateStore
import de.heikoseeberger.akkahttpjackson.JacksonSupport

object QueriesAkkaHttpResource extends JacksonSupport {

  def storeRoutes(predictions: ReadableModelStateStore): Route =
    get {
      path("stats") {
        val info: ModelToServeStats = predictions.getCurrentServingInfo
        complete(info)
      }
    }
}
