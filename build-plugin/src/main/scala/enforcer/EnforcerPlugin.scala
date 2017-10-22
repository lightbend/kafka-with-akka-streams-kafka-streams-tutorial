package sbtenforcer

import sbt._, sbtdynver.DynVerPlugin, sbtassembly.{AssemblyPlugin, Assembly}
import Keys._, Defaults._

object EnforcerPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin && sbtdynver.DynVerPlugin && sbtassembly.AssemblyPlugin
  override def trigger = allRequirements

  def doError[T]():T =
    throw new IllegalStateException("""|One of the follwing is true:
                                       | 1. You have uncommmited changes (unclean directory) - Fix: commit your changes and set a tag on HEAD.
                                       | 2. You have a clean directory but no tag on HEAD - Fix: tag the head with a version that satisfies the regex: 'v[0-9][^+]*'
                                       | 3. You have uncommmited changes (a dirty directory) but have not set `allowSnapshot` to `true` - Fix: `set (allowSnapshot in ThisBuild) := true`""".stripMargin)

  object autoImport {
    val allowSnapshot = settingKey[Boolean]("Allow uncommmited changes and emit a snapshot version")
    val canPublish = taskKey[Boolean]("Does the current have any un-committed files?")
    val enforcerPackageBinMappings = taskKey[Seq[(File, String)]]("The original `packageBin` task")
  }
  import autoImport._
  import sbtassembly.AssemblyKeys._

  override def buildSettings: Seq[Setting[_]] = Seq(
    allowSnapshot := false
  )

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      canPublish := {
        allowSnapshot.value ||  !(isSnapshot.value)
      },
      assembledMappings in assembly := {
        if (!canPublish.value) doError()
        else Assembly.assembledMappingsTask(assembly).value
      }
    ) ++
    inConfig(Compile)(baseEnforcerSettings)

  def baseEnforcerSettings: Seq[Setting[_]] = {
    packageTaskSettings(packageBin, enforcerPackageBinMappings) ++
    Seq(
      enforcerPackageBinMappings := {
        if (!canPublish.value) doError()
        else packageBinMappings.value
      }
    )
  }
}
