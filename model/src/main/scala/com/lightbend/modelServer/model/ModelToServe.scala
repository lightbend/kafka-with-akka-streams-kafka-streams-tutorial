package com.lightbend.modelServer.model

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
    println("Parsed new mode")
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
}

case class ModelWithDescriptor(model: Model, descriptor: ModelToServe){}

object ModelWithDescriptor {

  private val factories = Map(
    ModelDescriptor.ModelType.PMML.name -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW.name -> TensorFlowModel
  )

  def fromModelToServe(descriptor : ModelToServe): Try[ModelWithDescriptor] = Try{
    println(s"New model - $descriptor")
    factories.get(descriptor.modelType.name) match {
      case Some(factory) => ModelWithDescriptor(factory.create(descriptor),descriptor)
      case _ => throw new Throwable("Undefined model type")
    }
  }
}