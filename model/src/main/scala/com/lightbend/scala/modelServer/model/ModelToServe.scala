package com.lightbend.scala.modelServer.model

import com.lightbend.model.modeldescriptor.ModelDescriptor

import scala.util.Try


/**
 * A wrapper for metadata about a model.
 * Created by boris on 5/8/17.
 */
case class ModelToServe(name: String, description: String,
  modelType: ModelDescriptor.ModelType, model: Array[Byte], dataType: String) {}

case class ServingResult(processed : Boolean, result: Double = .0, duration: Long = 0l)

object ServingResult{
  val noModel = ServingResult(processed = false)
  def apply(result: Double, duration: Long): ServingResult = ServingResult(processed = true, result, duration)
}

object ModelToServe {
  def fromByteArray(message: Array[Byte]): Try[ModelToServe] = Try {
    val m = ModelDescriptor.parseFrom(message)
    if (m.messageContent.isData) {
      new ModelToServe(m.name, m.description, m.modeltype, m.getData.toByteArray, m.dataType)
    } else {
      throw new Exception("Location based is not yet supported")
    }
  }
}

