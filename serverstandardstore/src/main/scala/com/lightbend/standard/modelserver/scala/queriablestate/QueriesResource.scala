package com.lightbend.standard.modelserver.scala.queriablestate

import javax.ws.rs.NotFoundException

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.lightbend.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.standard.modelserver.scala.store.StoreState
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
          val info = metadataService.streamsMetadataForStore(STORE_NAME, port)
          complete(info)
        }
        path("value") {
          val store = streams.store(ApplicationKafkaParameters.STORE_NAME, QueryableStoreTypes.keyValueStore[Integer,StoreState])
          if (store == null) throw new NotFoundException
          val info = store.get(STORE_ID).currentState
          complete(info)
        }
      }
    }
  }
}
