
import Dependencies._

// scalaVersion in ThisBuild := "2.11.12"
scalaVersion in ThisBuild := "2.12.4"

scalacOptions in ThisBuild := Seq("-Xexperimental", "-Xlint:_", "-unchecked", "-deprecation", "-feature", "-target:jvm-1.8")
// Unfortunately there are lots of annoying warnings from the generated Protobuf code:
// javacOptions in ThisBuild := Seq("-Xlint:all")

lazy val protobufs = (project in file("./protobufs"))
    .settings(
      PB.targets in Compile := Seq(
        PB.gens.java -> (sourceManaged in Compile).value,
        scalapb.gen(javaConversions=true) -> (sourceManaged in Compile).value
      )
    )

lazy val client = (project in file("./client"))
  .settings(libraryDependencies ++=  Seq(Dependencies.kafka, Dependencies.curator))
  .dependsOn(protobufs, configuration)

lazy val model = (project in file("./model"))
  .settings(libraryDependencies ++= Dependencies.modelsDependencies)
  .dependsOn(protobufs)

lazy val kafkaStreamsModelServer = (project in file("./kafkaStreamsModelServer"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.1",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1"
  )
  .settings(libraryDependencies ++= Seq(Dependencies.kafkastreams, Dependencies.kafkastreamsScala) ++ Dependencies.webDependencies ++ Dependencies.akkHTTPPSupport)
  .dependsOn(model, configuration)

lazy val akkaStreamsModelServer = (project in file("./akkaStreamsModelServer"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.1",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1"
  )
  .settings(libraryDependencies ++= Dependencies.kafkabaseDependencies ++ Dependencies.akkaServerDependencies
    ++ Dependencies.modelsDependencies)
  .dependsOn(model, configuration)


lazy val configuration = (project in file("./configuration"))

lazy val akkaKafkaTutorial = (project in file(".")).
  aggregate(protobufs, client, model, configuration,
    kafkaStreamsModelServer,
    akkaStreamsModelServer)

addCommandAlias("run_client", "client/runMain com.lightbend.scala.kafka.client.DataProvider")
addCommandAlias("run_reader", "client/runMain com.lightbend.scala.kafka.client.DataReader")
addCommandAlias("run_java_akka_server",   "akkaStreamsModelServer/runMain com.lightbend.java.akkastreams.modelserver.AkkaModelServer")
addCommandAlias("run_scala_akka_server",  "akkaStreamsModelServer/runMain com.lightbend.scala.akkastream.modelserver.AkkaModelServer")
addCommandAlias("run_java_kafka_server",  "kafkaStreamsModelServer/runMain com.lightbend.java.kafkastreams.modelserver.KafkaModelServer")
addCommandAlias("run_scala_kafka_server", "kafkaStreamsModelServer/runMain com.lightbend.scala.kafkastreams.modelserver.KafkaModelServer")
