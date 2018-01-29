package com.lightbend.scala.modelServer.model

import com.lightbend.model.modeldescriptor.ModelDescriptor

import scala.collection.Map
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
