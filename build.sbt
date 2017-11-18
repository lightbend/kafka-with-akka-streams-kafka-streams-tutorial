
import Dependencies._

scalaVersion in ThisBuild := "2.11.11"

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

lazy val naiveserver = (project in file("./naiveserver"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.8.4",
            dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.4"
  )
  .settings(libraryDependencies ++= Seq(Dependencies.kafkastreams) ++ Dependencies.webDependencies ++ Dependencies.akkHTTPPSupport)
  .dependsOn(model, configuration)

lazy val serverstandardstore = (project in file("./serverstandardstore"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.8.4",
            dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.4"
  )
  .settings(libraryDependencies ++= Seq(Dependencies.kafkastreams) ++ Dependencies.webDependencies ++ Dependencies.akkHTTPPSupport)
  .dependsOn(model, configuration)

lazy val server = (project in file("./server"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.8.4",
            dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.4"
  )
  .settings(libraryDependencies ++= Seq(Dependencies.kafkastreams) ++ Dependencies.webDependencies ++ Dependencies.akkHTTPPSupport)
  .dependsOn(model, configuration)

lazy val akkaServer = (project in file("./akkaserver"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.8.4",
            dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.4"
  )
  .settings(libraryDependencies ++= Dependencies.kafkabaseDependencies ++ Dependencies.akkaServerDependencies
    ++ Dependencies.modelsDependencies ++ Seq(Dependencies.curator))
  .dependsOn(model, configuration)

lazy val configuration = (project in file("./configuration"))

lazy val akkakafkatutorial = (project in file(".")).
  aggregate(protobufs, client, model, configuration, server, akkaServer)
