package com.lightbend.standard.modelserver.scala.store

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import java.util

import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.modelServer.model.PMML.PMMLModel
import com.lightbend.modelServer.model._
import com.lightbend.modelServer.model.tensorflow.TensorFlowModel
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

class ModelStateSerde extends Serde[StoreState] {

  private var mserializer = new ModelStateSerializer()
  private var mdeserializer = new ModelStateDeserializer()

  override def deserializer() = mdeserializer

  override def serializer() = mserializer

  override def configure(configs: util.Map[String, _], isKey: Boolean) = {}

  override def close() = {}
}

object ModelStateDeserializer {
  private val factories = Map(
    ModelDescriptor.ModelType.PMML.index -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW.index -> TensorFlowModel
  )
}

class ModelStateDeserializer extends Deserializer[StoreState] {

  import ModelStateDeserializer._

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): StoreState = {
    if(data != null) {
      val input = new DataInputStream(new ByteArrayInputStream(data))
      new StoreState(readModel(input), readModel(input), readServingInfo(input), readServingInfo(input))
    }
    else new StoreState()
  }

  override def close(): Unit = {}

  private def readModel(input: DataInputStream) : Option[Model] = {
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
}

class ModelStateSerializer extends Serializer[StoreState] {

  private val bos = new ByteArrayOutputStream()

  override def serialize(topic: String, state: StoreState): Array[Byte] = {
    bos.reset()
    val output = new DataOutputStream(bos)
    writeModel(state.currentModel, output)
    writeModel(state.newModel, output)
    writeServingInfo(state.currentState, output)
    writeServingInfo(state.newState, output)
    try {
      output.flush()
      output.close()
    } catch {
      case t: Throwable =>
    }
    bos.toByteArray
  }

  private def writeModel(omodel: Option[Model], output: DataOutputStream): Unit = {
    omodel match {
      case Some(model) => try {
        val bytes = model.toBytes()
        output.writeLong(bytes.length)
        output.writeLong(model.getType)
        output.write(bytes)
      } catch {
        case t: Throwable =>
          System.out.println("Error Serializing model")
          t.printStackTrace()
      }
      case _ => output.writeLong(0)
    }
  }

  private def writeServingInfo(oservingInfo: Option[ModelToServeStats], output: DataOutputStream): Unit = {
    oservingInfo match {
      case Some(servingInfo) => try{
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
      case _ => output.writeLong(0)
    }
  }

  override def close(): Unit = {}

  override def configure(configs: util.Map[String, _], isKey: Boolean) = {}
}
