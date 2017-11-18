package com.lightbend.naive.modelserver.scala.queriablestate

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.lightbend.modelServer.model.ModelToServeStats
import com.lightbend.naive.modelserver.scala.store.StoreState
import de.heikoseeberger.akkahttpjackson.JacksonSupport

object QueriesResource extends JacksonSupport {

  def storeRoutes(): Route =
    get {
      pathPrefix("state") {
        path("value") {
          val info: ModelToServeStats = StoreState().currentState.getOrElse(ModelToServeStats())
          complete(info)
        }
      }
    }
}
