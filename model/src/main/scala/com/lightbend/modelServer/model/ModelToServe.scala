package com.lightbend.modelServer.model

import java.io.{DataInputStream, DataOutputStream}

import com.lightbend.model.modeldescriptor.ModelDescriptor

import scala.collection.Map
import com.lightbend.modelServer.model.PMML.PMMLModel
import com.lightbend.modelServer.model.tensorflow.TensorFlowModel

import scala.util.Try

/**
 * Created by boris on 5/8/17.
 */
object ModelToServe {
  def fromByteArray(message: Array[Byte]): Try[ModelToServe] = Try {
    val m = ModelDescriptor.parseFrom(message)
    m.messageContent.isData match {
      case true => new ModelToServe(m.name, m.description, m.modeltype, m.getData.toByteArray, m.dataType)
      case _ => throw new Exception("Location based is not yet supported")
    }
  }
}

case class ModelToServe(name: String, description: String,
  modelType: ModelDescriptor.ModelType, model: Array[Byte], dataType: String) {}

case class ModelToServeStats(name: String = "", description: String = "",
    since: Long = 0, var usage: Long = 0, var duration: Double = .0,
    var min: Long = Long.MaxValue, var max: Long = Long.MinValue) {
  def this(m: ModelToServe) = this(m.name, m.description, System.currentTimeMillis())
  def incrementUsage(execution: Long): ModelToServeStats = {
    usage = usage + 1
    duration = duration + execution
    if (execution < min) min = execution
    if (execution > max) max = execution
    this
  }
}

object ModelToServeStats {
  val empty = ModelToServeStats("None", "None", 0, 0, .0, 0, 0)

  def readServingInfo(input: DataInputStream) : Option[ModelToServeStats] = {
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

  def writeServingInfo(output: DataOutputStream, servingInfo: ModelToServeStats ): Unit = {
    if(servingInfo == null)
      output.writeLong(0)
    else {
      try {
        output.writeLong(5)
        output.writeUTF(servingInfo.description)
        output.writeUTF(servingInfo.name)
        output.writeLong(servingInfo.since)
        output.writeLong(servingInfo.usage)
        output.writeDouble(servingInfo.duration)
        output.writeLong(servingInfo.min)
        output.writeLong(servingInfo.max)
      } catch {
        case t: Throwable =>
          System.out.println("Error Serializing servingInfo")
          t.printStackTrace()
      }
    }
  }
}

case class ModelWithDescriptor(model: Model, descriptor: ModelToServe){}

object ModelWithDescriptor {

  private val factories = Map(
    ModelDescriptor.ModelType.PMML.name -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW.name -> TensorFlowModel
  )

  private val factoriesInt = Map(
    ModelDescriptor.ModelType.PMML.index -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW.index -> TensorFlowModel
  )

  def fromModelToServe(descriptor : ModelToServe): Try[ModelWithDescriptor] = Try{
    println(s"New model - $descriptor")
    factories.get(descriptor.modelType.name) match {
      case Some(factory) => ModelWithDescriptor(factory.create(descriptor),descriptor)
      case _ => throw new Throwable("Undefined model type")
    }
  }

  def readModel(input : DataInputStream) : Option[Model] = {
    input.readLong.toInt match{
      case length if length > 0 => {
        val `type` = input.readLong.toInt
        val bytes = new Array[Byte](length)
        input.read(bytes)
        factoriesInt.get(`type`) match{
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

  def writeModel(output : DataOutputStream, model: Model) : Unit = {
    if(model == null)
      output.writeLong(0l)
    else {
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
  }
}