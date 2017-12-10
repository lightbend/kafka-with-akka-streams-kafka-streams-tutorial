package com.lightbend.modelserver.actor.persistence


import java.io.{DataInputStream, DataOutputStream, FileInputStream, FileOutputStream, File}

import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.modelServer.model.{Model, ModelToServeStats}
import com.lightbend.modelServer.model.PMML.PMMLModel
import com.lightbend.modelServer.model.tensorflow.TensorFlowModel

object FilePersistence {

  private final val basDir = "persistence"

  private val factories = Map(
    ModelDescriptor.ModelType.PMML.index -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW.index -> TensorFlowModel
  )

  def restoreState(dataType: String) : (Option[Model], Option[ModelToServeStats]) = {
    getDataInputStream(dataType) match {
      case Some(input) => (readModel(input), readServingInfo(input))
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

  private def readModel(input : DataInputStream) : Option[Model] = {
    input.readLong.toInt match{
      case length if length > 0 => {
        val `type` = input.readLong.toInt
        val bytes = new Array[Byte](length)
        input.read(bytes)
        factories.get(`type`) match{
          case Some(factory) => try {
            Some(factory.restore(bytes))
          }
          catch {
            case t: Throwable =>
              System.out.println("Error Deserializing model")
              t.printStackTrace()
              None
          }
          case _ => None
        }
      }
      case _ => None
    }
  }

  private def readServingInfo(input: DataInputStream) : Option[ModelToServeStats] = {
    input.readLong match {
      case length if length > 0 => {
        try {
          Some(ModelToServeStats(input.readUTF, input.readUTF, input.readLong, input.readLong, input.readDouble, input.readLong, input.readLong))
        } catch {
          case t: Throwable =>
            System.out.println("Error Deserializing serving info")
            t.printStackTrace()
            None
        }
      }
      case _ => None
    }
  }


  def saveState(dataType: String, model: Model, servingInfo: ModelToServeStats) : Unit = {
    val output = getDataOutputStream(dataType)
    writeModel(output, model)
    writeServingInfo(output, servingInfo)
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

  private def writeModel(output : DataOutputStream, model: Model) : Unit = {
    try {
      val bytes = model.toBytes()
      output.writeLong(bytes.length)
      output.writeLong(model.getType)
      output.write(bytes)
    } catch {
      case t: Throwable =>
        System.out.println("Error Serializing model")
        t.printStackTrace()
    }
  }

  private def writeServingInfo(output: DataOutputStream, servingInfo: ModelToServeStats ): Unit = {
    try{
      output.writeLong(5)
      output.writeUTF(servingInfo.description)
      output.writeUTF(servingInfo.name)
      output.writeLong(servingInfo.since)
      output.writeLong(servingInfo.usage)
      output.writeDouble(servingInfo.duration)
      output.writeLong(servingInfo.min)
      output.writeLong(servingInfo.max)
    }catch {
      case t: Throwable =>
        System.out.println("Error Serializing servingInfo")
        t.printStackTrace()
    }
  }
}
