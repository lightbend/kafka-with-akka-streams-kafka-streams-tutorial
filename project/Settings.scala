/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.Keys._
import sbt._

import scala.language.postfixOps

object Settings extends Build {

  lazy val buildSettings = Seq(
    name := "Kafka/Akka stream model server",
    normalizedName := "model server",
    organization := "com.lightbend.modelserver",
    organizationHomepage := Some(url("https://github.com/typesafehub/fdp-kafka-streams-modelServer")),
    scalaVersion := Versions.Scala,
    homepage := Some(url("https://github.com/typesafehub/fdp-kafka-streams-modelServer")),
    licenses := Seq(("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0")))
  )

  override lazy val settings = super.settings ++ buildSettings

  val parentSettings = buildSettings ++ Seq(
    publishArtifact := false,
    publish := {}
  )

  lazy val defaultSettings = testSettings ++ sigarSettings ++ Seq(
    autoCompilerPlugins := true,
    // removed "-Xfatal-warnings" as temporary workaround for log4j fatal error.
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      s"-target:jvm-${Versions.JDK}",
      "-feature",
      "-deprecation",
      "-unchecked",
      // "-Xfatal-warnings",
      "-Xlint",
      "-language:_",
      "-Yinline-warnings",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-infer-any",
      "-Ywarn-unused-import"),
    javacOptions in Compile ++= Seq("-encoding", "UTF-8", "-source", Versions.JDK, "-target", Versions.JDK, "-Xlint:deprecation", "-Xlint:unchecked"),
    run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)),
    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
    parallelExecution in ThisBuild := false,
    parallelExecution in Global := false
  )

  val tests = inConfig(Test)(Defaults.testTasks) ++ inConfig(IntegrationTest)(Defaults.itSettings)

  val testOptionSettings = Seq(
    Tests.Argument(TestFrameworks.ScalaTest, "-oDF")
  )

  lazy val testSettings = tests ++ Seq(
    parallelExecution in Test := false,
    parallelExecution in IntegrationTest := false,
    testOptions in Test ++= testOptionSettings,
    testOptions in IntegrationTest ++= testOptionSettings,
    baseDirectory in Test := baseDirectory.value.getParentFile(),
    fork in Test := true,
    fork in IntegrationTest := true,
    (compile in IntegrationTest) <<= (compile in Test, compile in IntegrationTest) map { (_, c) => c },
    managedClasspath in IntegrationTest <<= Classpaths.concat(managedClasspath in IntegrationTest, exportedProducts in Test)
  )

  lazy val sigarSettings = Seq(
    unmanagedSourceDirectories in (Compile,run) += baseDirectory.value.getParentFile / "sigar",
    javaOptions in run ++= {
      System.setProperty("java.library.path", file("./sigar").getAbsolutePath)
      Seq("-Xms128m", "-Xmx1024m")
    })

}
