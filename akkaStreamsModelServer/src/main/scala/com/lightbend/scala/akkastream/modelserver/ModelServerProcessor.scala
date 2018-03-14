package com.lightbend.scala.akkastream.modelserver

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.Timeout
import akka.pattern.ask
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.akkastream.modelserver.actors.ModelServingManager
import com.lightbend.scala.akkastream.modelserver.stage.{ModelStage, ModelStateStore}
import com.lightbend.scala.akkastream.queryablestate.actors.RestServiceActors
import com.lightbend.scala.akkastream.queryablestate.inmemory.RestServiceInMemory
import com.lightbend.scala.modelServer.model.{ModelWithDescriptor, ServingResult}

import scala.concurrent.duration._

trait ModelServerProcessor {
  def createStreams(dataStream: Source[WineRecord, Consumer.Control], modelStream: Source[ModelWithDescriptor, Consumer.Control])
                   (implicit system: ActorSystem, materializer: ActorMaterializer): Unit
}

object ActorModelServerProcessor extends ModelServerProcessor {

  def createStreams(dataStream: Source[WineRecord, Consumer.Control], modelStream: Source[ModelWithDescriptor, Consumer.Control])
                   (implicit system: ActorSystem, materializer: ActorMaterializer): Unit = {

    println("*** Using the Actor-based model server implementation ***")
    implicit val executionContext = system.dispatcher
    implicit val askTimeout = Timeout(30.seconds)

    val modelserver = system.actorOf(ModelServingManager.props)

    // Model stream processing
    modelStream
      .mapAsync(1)(elem => modelserver ? elem)
      .runWith(Sink.ignore) // run the stream, we do not read the results directly

    // Data stream processing
    dataStream
      .mapAsync(1)(elem => (modelserver ? elem).mapTo[ServingResult])
      .runForeach(result => {
        result.processed match {
          case true => println(s"Calculated quality - ${result.result} calculated in ${result.duration} ms")
          case _ => println("No model available - skipping")
        }
      })

    // Rest Server
    RestServiceActors.startRest(modelserver)
  }
}

object CustomStageModelServerProcessor extends ModelServerProcessor {

  def createStreams(dataStream: Source[WineRecord, Consumer.Control], modelStream: Source[ModelWithDescriptor, Consumer.Control])
                   (implicit system: ActorSystem, materializer: ActorMaterializer): Unit = {

    println("*** Using the Custom Stage model server implementation ***")
    implicit val executionContext = system.dispatcher

    val modelPredictions: Source[Option[Double], ModelStateStore] =
      dataStream.viaMat(new ModelStage)(Keep.right).map { result =>
        result.processed match {
          case true => println(s"Calculated quality - ${result.result} calculated in ${result.duration} ms"); Some(result.result)
          case _ => println ("No model available - skipping"); None
        }
      }

    val modelStateStore: ModelStateStore =
      modelPredictions
        .to(Sink.ignore)  // we do not read the results directly
        .run()            // we run the stream, materializing the stage's StateStore

    // model stream
    modelStream.runForeach(modelStateStore.setModel)

    RestServiceInMemory.startRest(modelStateStore)
  }
}
