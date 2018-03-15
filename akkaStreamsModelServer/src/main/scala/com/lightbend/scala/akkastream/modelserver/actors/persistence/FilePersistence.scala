package com.lightbend.scala.akkastream.modelserver.actors.persistence

import java.io._

import com.lightbend.scala.modelServer.model.{Model, ModelToServeStats, ModelWithDescriptor}

object FilePersistence {

  private final val basDir = "persistence"

  def restoreState(dataType: String) : (Option[Model], Option[ModelToServeStats]) = {
    getDataInputStream(dataType) match {
      case Some(input) => (ModelWithDescriptor.readModel(input), ModelToServeStats.readServingInfo(input))
      case _ => (None, None)
    }
   }

  private def getDataInputStream(fileName: String) : Option[DataInputStream] = {
    val file = new File(basDir + "/" + fileName)
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

  private def getDataOutputStream(fileName: String) : DataOutputStream = {

    val dir = new File(basDir)
    if(!dir.exists()) {
      dir.mkdir()
    }
    val file = new File(dir, fileName)
    if(!file.exists())
      file.createNewFile()
    new DataOutputStream(new FileOutputStream(file))
  }
}
