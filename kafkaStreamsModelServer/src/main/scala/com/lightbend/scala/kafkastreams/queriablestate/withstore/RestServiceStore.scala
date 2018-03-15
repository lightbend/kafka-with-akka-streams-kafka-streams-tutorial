package com.lightbend.scala.kafkastreams.queriablestate.withstore

import javax.ws.rs.NotFoundException
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.lightbend.scala.kafkastreams.queriablestate.MetadataService
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters._
import com.lightbend.scala.kafkastreams.store.StoreState
import com.lightbend.scala.kafkastreams.store.store.custom.ModelStateStoreType
import de.heikoseeberger.akkahttpjackson.JacksonSupport
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.{QueryableStoreType, QueryableStoreTypes}

import scala.concurrent.duration._

object RestServiceStore {

  implicit val system = ActorSystem("ModelServing")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10.seconds)
  val host = "127.0.0.1"
  val port = 8888

  def startRestProxy(streams: KafkaStreams, port: Int, storeType : String) = {

    val routes: Route = QueriesResource.storeRoutes(streams, port, storeType)
    Http().bindAndHandle(routes, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
      case ex =>
        println(s"Models observer could not bind to $host:$port - ${ex.getMessage}")
    }
  }
}

object QueriesResource extends JacksonSupport {

  private val customStoreType = new ModelStateStoreType()
  private val standardStoreType = QueryableStoreTypes.keyValueStore[Integer,StoreState]

  def storeRoutes(streams: KafkaStreams, port : Int, storeType : String): Route = {
    val metadataService = new MetadataService(streams)
    get {
      pathPrefix("state") {
        path("instances") {
          complete(
            metadataService.streamsMetadataForStore(STORE_NAME, port)
          )
        } ~
          path("value") {
            storeType match {
              case "custom" =>
                val store = streams.store(STORE_NAME, customStoreType)
                if (store == null) throw new NotFoundException
                complete(store.getCurrentServingInfo)
              case _ =>
                val store = streams.store(STORE_NAME, standardStoreType)
                if (store == null) throw new NotFoundException
                complete(store.get(STORE_ID).currentState)
            }
          }
        }
    }
  }
}