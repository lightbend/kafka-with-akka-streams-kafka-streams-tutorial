sbtPlugin := true

name := "enforcer"

organization := "com.lightbend.fdp"

version := "1.0"

libraryDependencies ++= Seq(
  Defaults.sbtPluginExtra("com.dwijnand" % "sbt-dynver" % "2.0.0", "0.13", "2.10"),
  Defaults.sbtPluginExtra("com.eed3si9n" % "sbt-assembly" % "0.14.5", "0.13", "2.10")
)