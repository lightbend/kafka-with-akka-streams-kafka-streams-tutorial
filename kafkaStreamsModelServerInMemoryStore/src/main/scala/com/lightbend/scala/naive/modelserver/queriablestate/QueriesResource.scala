package com.lightbend.scala.naive.modelserver.queriablestate

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.lightbend.scala.modelServer.model.ModelToServeStats
import com.lightbend.scala.naive.modelserver.store.StoreState
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
