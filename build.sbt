
import Dependencies._
import deployssh.DeploySSH._

allowSnapshot in ThisBuild := true

scalaVersion in ThisBuild := "2.11.11"

lazy val protobufs = (project in file("./protobufs"))
    .settings(
      PB.targets in Compile := Seq( 
        PB.gens.java -> (sourceManaged in Compile).value,
        scalapb.gen(javaConversions=true) -> (sourceManaged in Compile).value
      )
    )

lazy val client = (project in file("./client"))
  .settings(
    buildInfoPackage := "build",
    mainClass in Compile := Some("com.lightbend.kafka.DataProvider"),
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "Model Server Loaders",
    packageDescription := "Model Server Loaders",
    deployResourceConfigFiles ++= Seq("deploy.conf"),
    deployArtifacts ++= Seq(
      ArtifactSSH((packageZipTarball in Universal).value, "/var/www/html/")
    )
  )
  .settings(libraryDependencies ++= Dependencies.kafkabaseDependencies)
  .dependsOn(protobufs, configuration)
  .enablePlugins(DeploySSH)
  .enablePlugins(JavaAppPackaging)

lazy val model = (project in file("./model"))
  .settings(libraryDependencies ++= Dependencies.modelsDependencies)
  .dependsOn(protobufs)

lazy val server = (project in file("./server"))
  .settings(
    buildInfoPackage := "build",
    mainClass in Compile := Some("com.lightbend.modelserver.withstore.ModelServerWithStore"),
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "Model Server Kafka Streams",
    packageDescription := "Model Server Kafka Streams",
    deployResourceConfigFiles ++= Seq("deploy.conf"),
    deployArtifacts ++= Seq(
      ArtifactSSH((packageZipTarball in Universal).value, "/var/www/html/")
    )
  )
  .settings(libraryDependencies ++= Dependencies.kafkaDependencies ++ Dependencies.webDependencies)
  .dependsOn(model, configuration)
  .enablePlugins(DeploySSH)
  .enablePlugins(JavaAppPackaging)

lazy val akkaServer = (project in file("./akkaserver"))
  .settings(
    buildInfoPackage := "build",
    mainClass in Compile := Some("com.lightbend.modelServer.modelServer.AkkaModelServer"),
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "Model Server Akka Streams",
    packageDescription := "Model Server Akka Streams",
    deployResourceConfigFiles ++= Seq("deploy.conf"),
    deployArtifacts ++= Seq(
      ArtifactSSH((packageZipTarball in Universal).value, "/var/www/html/")
    )
  )    .settings(libraryDependencies ++= Dependencies.kafkaDependencies ++ Dependencies.akkaServerDependencies
    ++ Dependencies.modelsDependencies ++ Seq(Dependencies.curator))
  .dependsOn(protobufs, configuration)
  .enablePlugins(DeploySSH)
  .enablePlugins(JavaAppPackaging)

lazy val configuration = (project in file("./configuration"))
  .settings(libraryDependencies ++= Seq(influxDBClient, codecBase64))

lazy val modelserver = (project in file(".")).
  aggregate(protobufs, client, model, configuration, server, akkaServer)

