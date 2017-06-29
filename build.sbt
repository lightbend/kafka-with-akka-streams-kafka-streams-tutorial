
name := "KafkaStreamsModelServer"

version := "1.0"

scalaVersion := "2.11.8"

lazy val kafka = "0.10.2.1"
lazy val tensorflow = "1.1.0"
lazy val PMML = "1.3.5"
lazy val chill = "0.9.2"
lazy val jetty = "9.2.12.v20150709"
lazy val jackson = "2.8.8"
lazy val jersey = "2.25"
lazy val gson = "2.6.2"
lazy val wsrs = "2.0.1"


PB.targets in Compile := Seq(
  PB.gens.java -> (sourceManaged in Compile).value,
  scalapb.gen(javaConversions=true) -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka_2.11" % kafka,
  "org.apache.kafka" % "kafka-clients" % kafka,
  "org.apache.kafka" % "kafka-streams" % kafka,

  "com.google.code.gson" % "gson" % gson,
  "org.glassfish.jersey.containers" % "jersey-container-servlet-core" % jersey,
  "org.glassfish.jersey.media" % "jersey-media-json-jackson" % jersey,
  "org.eclipse.jetty" % "jetty-server" % jetty,
  "org.eclipse.jetty" % "jetty-servlet" % jetty,
  "javax.ws.rs" % "javax.ws.rs-api" % wsrs,

  "org.tensorflow" % "tensorflow" % tensorflow,
  "org.jpmml" % "pmml-evaluator" % PMML,
  "org.jpmml" % "pmml-evaluator-extension" % PMML
)
