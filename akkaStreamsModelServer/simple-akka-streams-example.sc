// To run this script, run these commands:
// 1. sbt
// 2. project akkaStreamsCustomStage
// 3. console
// Then copy and paste this code to the scala prompt
// Loosely adapted from:
// https://doc.akka.io/docs/akka/2.5.5/scala/stream/stream-quickstart.html

import akka.stream._
import akka.stream.scaladsl._
import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import scala.concurrent._
import scala.concurrent.duration._

implicit val system = ActorSystem("QuickStart")
implicit val materializer = ActorMaterializer()

val source: Source[Int, NotUsed] = Source(1 to 10)
val factorials = source.scan(BigInt(1)) ((acc, next) => acc * next)
factorials.runWith(Sink.foreach(println))
