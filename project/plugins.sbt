logLevel := Level.Warn

resolvers += Resolver.sonatypeRepo("releases")

resolvers += "Bintray Repository" at "https://dl.bintray.com/shmishleniy/"

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.4")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")
addSbtPlugin("org.scalariform" %% "sbt-scalariform" % "1.6.0")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")
addSbtPlugin("com.eed3si9n" %% "sbt-buildinfo" % "0.6.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0-M8")
addSbtPlugin("com.github.shmishleniy" %% "sbt-deploy-ssh" % "0.1.3")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "2.0.0")

dependsOn(RootProject(file("./build-plugin/").toURI))
