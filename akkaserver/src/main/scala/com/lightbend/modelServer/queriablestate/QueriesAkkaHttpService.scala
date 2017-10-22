package com.lightbend.modelServer.queriablestate

import java.util.OptionalDouble

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{ HttpApp, Route }
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import com.lightbend.model.Winerecord
import com.lightbend.modelServer.ModelToServeStats
import com.lightbend.modelServer.modelServer.ReadableModelStateStore
import de.heikoseeberger.akkahttpjackson.JacksonSupport

// FIXME seems this API does not make much sense if it's not
class QueriesAkkaHttpService(
    predictions: Source[(Winerecord.WineRecord, Option[Double]), ReadableModelStateStore]
) extends HttpApp with JacksonSupport {

  def this(predictions: akka.stream.javadsl.Source[akka.japi.Pair[Winerecord.WineRecord, OptionalDouble], ReadableModelStateStore]) {
    this(predictions.asScala.map(p => p.first -> (if (p.second.isPresent) Some(p.second.getAsDouble) else None)))
  }

  def startServer(host: String, port: Int, system: ActorSystem): Unit = {
    super.startServer(host, port, ServerSettings(system.settings.config), system)
  }

  private var readableModelStateStore: ReadableModelStateStore = _

  override def postHttpBinding(binding: Http.ServerBinding): Unit = {
    implicit val system = systemReference.get()
    implicit val mat = ActorMaterializer()

    readableModelStateStore = predictions.to(Sink.foreach {
      case (wine, score) => // print it, or do anything you want with them...
    }).run()
  }

  override protected def routes: Route =
    (get & pathPrefix("state")) {
      //      instancesRoutes ~
      storeRoutes
    }
  /*
  def instancesRoutes: Route =
    pathPrefix("instances") {
      path(StoreName) { storeName =>
        complete(???) // FIXME not sure what to do here... seems that service only makes sense if using kafka streams?
      } ~
        pathEnd {
          complete(???) // FIXME not sure what to do here... seems that service only makes sense if using kafka streams?
        }
    } */

  def storeRoutes: Route =
    path(StoreName / "value") { storeName =>
      // TODO so we should start one streaming instance for each of the store names?
      val info: ModelToServeStats = readableModelStateStore.getCurrentServingInfo
      complete(info)
    }

  private def StoreName = Segment
}