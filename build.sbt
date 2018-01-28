
import Dependencies._

scalaVersion in ThisBuild := "2.11.8"
// scalaVersion in ThisBuild := "2.12.4"

scalacOptions in ThisBuild := Seq("-Xexperimental", "-unchecked", "-deprecation", "-feature")
javaOptions in ThisBuild := Seq("Xlint:unchecked")

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

lazy val kafkaStreamsModelServerInMemoryStore = (project in file("./kafkaStreamsModelServerInMemoryStore"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.1",
            dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1"
  )
  .settings(libraryDependencies ++= Seq(Dependencies.kafkastreams) ++ Dependencies.webDependencies ++ Dependencies.akkHTTPPSupport)
  .dependsOn(model, configuration, scalakafkastreamswrapper)

lazy val kafkaStreamsModelServerKVStore = (project in file("./kafkaStreamsModelServerKVStore"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.1",
            dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1"
  )
  .settings(libraryDependencies ++= Seq(Dependencies.kafkastreams) ++ Dependencies.webDependencies ++ Dependencies.akkHTTPPSupport)
  .dependsOn(model, configuration, scalakafkastreamswrapper)

lazy val kafkaStreamsModelServerCustomStore = (project in file("./kafkaStreamsModelServerCustomStore"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.1",
            dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1"
  )
  .settings(libraryDependencies ++= Seq(Dependencies.kafkastreams) ++ Dependencies.webDependencies ++ Dependencies.akkHTTPPSupport)
  .dependsOn(model, configuration, scalakafkastreamswrapper)

lazy val akkaStreamsBasic = (project in file("./akkaStreamsBasic"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.1",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1"
  )
  .settings(libraryDependencies ++= Dependencies.kafkabaseDependencies ++ Dependencies.akkaServerDependencies
    ++ Dependencies.modelsDependencies)
  .dependsOn(model, configuration)

lazy val akkaStreamsPersistent = (project in file("./akkaStreamsPersistent"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.1",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1"
  )
  .settings(libraryDependencies ++= Dependencies.kafkabaseDependencies ++ Dependencies.akkaServerDependencies
    ++ Dependencies.modelsDependencies)
  .dependsOn(model, configuration)

lazy val configuration = (project in file("./configuration"))

lazy val scalakafkastreamswrapper = (project in file("./scalakafkastreamswrapper"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.1",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1"
  )
  .settings(libraryDependencies ++= Seq(Dependencies.kafkastreams) ++ Dependencies.webDependencies ++ Dependencies.akkHTTPPSupport)


lazy val akkaKafkaTutorial = (project in file(".")).
  aggregate(protobufs, client, model, configuration,
    kafkaStreamsModelServerCustomStore,
    kafkaStreamsModelServerInMemoryStore,
    kafkaStreamsModelServerKVStore,
    akkaStreamsBasic,
    akkaStreamsPersistent,
    scalakafkastreamswrapper)
