package com.lightbend.scala.akkastream.modelserver.actors.persistence

import java.io._

import com.lightbend.scala.modelServer.model.{Model, ModelToServeStats, ModelWithDescriptor}

/**
  * Persists the state information to a file for quick recovery.
  * This state optionally includes the current {@link com.lightbend.scala.modelServer.model.Model} and
  * {@link com.lightbend.scala.modelServer.model.ModelToServeStats}.
  */
object FilePersistence {

  private final val baseDir = "persistence"

  def restoreState(dataType: String) : (Option[Model], Option[ModelToServeStats]) = getDataInputStream(dataType) match {
    case Some(input) => (ModelWithDescriptor.readModel(input), ModelToServeStats.readServingInfo(input))
    case _ => (None, None)
  }

  private def getDataInputStream(fileName: String): Option[DataInputStream] = {
    val file = new File(baseDir + "/" + fileName)
    file.exists() match {
      case true => Some(new DataInputStream(new FileInputStream(file)))
      case _ => None
    }
  }

  def saveState(dataType: String, model: Model, servingInfo: ModelToServeStats) : Unit = {
    val output = getDataOutputStream(dataType)
    ModelWithDescriptor.writeModel(output, model)
    ModelToServeStats.writeServingInfo(output, servingInfo)
    output.flush()
    output.close()
  }

  private def getDataOutputStream(fileName: String) = {

    val dir = new File(baseDir)
    if(!dir.exists()) dir.mkdir()
    val file = new File(dir, fileName)
    if(!file.exists())
      file.createNewFile()
    new DataOutputStream(new FileOutputStream(file))
  }
}
