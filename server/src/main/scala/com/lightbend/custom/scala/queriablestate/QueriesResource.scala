package com.lightbend.custom.scala.queriablestate

import javax.ws.rs.NotFoundException

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.lightbend.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.custom.scala.store.ModelStateStoreType
import de.heikoseeberger.akkahttpjackson.JacksonSupport
import org.apache.kafka.streams.KafkaStreams

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
          val store = streams.store(STORE_NAME, new ModelStateStoreType())
          if (store == null) throw new NotFoundException
           complete(store.getCurrentServingInfo)
        }
      }
    }
  }
}
