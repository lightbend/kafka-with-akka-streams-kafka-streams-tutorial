package com.lightbend.scala.modelServer.model.tensorflow

import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.model.winerecord.WineRecord
import org.tensorflow.{Graph, Session, Tensor}
import com.lightbend.scala.modelServer.model.{Model, ModelFactory, ModelToServe}

/**
 * Created by boris on 5/26/17.
 * Implementation of a model using TensorFlow for "Records".
 */
class TensorFlowModel(inputStream: Array[Byte]) extends Model {

  val graph = new Graph
  graph.importGraphDef(inputStream)
  val session = new Session(graph)

  override def score(record: WineRecord): Any = {

    val data = Array(
      record.fixedAcidity.toFloat,
      record.volatileAcidity.toFloat,
      record.citricAcid.toFloat,
      record.residualSugar.toFloat,
      record.chlorides.toFloat,
      record.freeSulfurDioxide.toFloat,
      record.totalSulfurDioxide.toFloat,
      record.density.toFloat,
      record.pH.toFloat,
      record.sulphates.toFloat,
      record.alcohol.toFloat
    )
    val modelInput = Tensor.create(Array(data))
    val result = session.runner.feed("dense_1_input", modelInput).fetch("dense_3/Sigmoid").run().get(0)
    val rshape = result.shape
    var rMatrix = Array.ofDim[Float](rshape(0).asInstanceOf[Int], rshape(1).asInstanceOf[Int])
    result.copyTo(rMatrix)
    var value = (0, rMatrix(0)(0))
    1 to (rshape(1).asInstanceOf[Int] - 1) foreach { i =>
      {
        if (rMatrix(0)(i) > value._2)
          value = (i, rMatrix(0)(i))
      }
    }
    value._1.toDouble
  }

  override def cleanup(): Unit = {
    try {
      session.close
    } catch {
      case t: Throwable => // Swallow
    }
    try {
      graph.close
    } catch {
      case t: Throwable => // Swallow
    }
  }

  override def toBytes(): Array[Byte] = graph.toGraphDef

  override def getType: Long = ModelDescriptor.ModelType.TENSORFLOW.value
}

object TensorFlowModel extends ModelFactory {
  def apply(inputStream: Array[Byte]): Option[TensorFlowModel] = {
    try {
      Some(new TensorFlowModel(inputStream))
    } catch {
      case t: Throwable => None
    }
  }

  override def create(input: ModelToServe): Model = {
    new TensorFlowModel(input.model)
  }

  override def restore(bytes: Array[Byte]): Model = new TensorFlowModel(bytes)
}
