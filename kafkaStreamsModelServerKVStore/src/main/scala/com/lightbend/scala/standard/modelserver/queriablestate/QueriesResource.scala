package com.lightbend.scala.standard.modelserver.scala.queriablestate

import javax.ws.rs.NotFoundException

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.scala.standard.modelserver.scala.store.StoreState
import de.heikoseeberger.akkahttpjackson.JacksonSupport
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.QueryableStoreTypes

object QueriesResource extends JacksonSupport {

  import ApplicationKafkaParameters._

  def storeRoutes(streams: KafkaStreams, port : Int): Route = {
    val metadataService = new MetadataService(streams)
    get {
      pathPrefix("state") {
        path("instances") {
          complete(
            metadataService.streamsMetadataForStore(STORE_NAME, port)
          )
        } ~
        path("value") {
          val store = streams.store(ApplicationKafkaParameters.STORE_NAME, QueryableStoreTypes.keyValueStore[Integer,StoreState])
          if (store == null) throw new NotFoundException
          complete(store.get(STORE_ID).currentState)
        }
      }
    }
  }
}
