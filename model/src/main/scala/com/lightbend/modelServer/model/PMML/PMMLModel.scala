package com.lightbend.modelServer.model.PMML

/**
 * Created by boris on 5/9/17.
 *
 * Class for PMML model
 */

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.model.winerecord.WineRecord
import org.dmg.pmml.{FieldName, PMML}
import org.jpmml.evaluator.visitors._
import org.jpmml.evaluator.{Computable, FieldValue, ModelEvaluatorFactory, TargetField}
import org.jpmml.model.PMMLUtil
import com.lightbend.modelServer.model.{Model, ModelFactory, ModelToServe}

import scala.collection.JavaConversions._
import scala.collection._

class PMMLModel(inputStream: Array[Byte]) extends Model {

  var arguments = mutable.Map[FieldName, FieldValue]()

  // Marshall PMML
  val pmml = PMMLUtil.unmarshal(new ByteArrayInputStream(inputStream))

  // Optimize model// Optimize model
  PMMLModel.optimize(pmml)

  // Create and verify evaluator
  val evaluator = ModelEvaluatorFactory.newInstance.newModelEvaluator(pmml)
  evaluator.verify()

  // Get input/target fields
  val inputFields = evaluator.getInputFields
  val target: TargetField = evaluator.getTargetFields.get(0)
  val tname = target.getName

  override def score(input: AnyVal): AnyVal = {
    val inputs = input.asInstanceOf[WineRecord]
    arguments.clear()
    inputFields.foreach(field => {
      arguments.put(field.getName, field.prepare(getValueByName(inputs, field.getName.getValue)))
    })

    // Calculate Output// Calculate Output
    val result = evaluator.evaluate(arguments)

    // Prepare output
    result.get(tname) match {
      case c: Computable => c.getResult.toString.toDouble
      case v: Any => v.asInstanceOf[Double]
    }
  }

  override def cleanup(): Unit = {}

  private def getValueByName(inputs: WineRecord, name: String): Double =
    PMMLModel.names.get(name) match {
      case Some(index) => {
        val v = inputs.getFieldByNumber(index + 1)
        v.asInstanceOf[Double]
      }
      case _ => .0
    }

  override def toBytes: Array[Byte] = {
    var stream = new ByteArrayOutputStream()
    PMMLUtil.marshal(pmml, stream)
    stream.toByteArray
  }

  override def getType: Long = ModelDescriptor.ModelType.PMML.value
}

object PMMLModel extends ModelFactory {

  private val optimizers = Array(new ExpressionOptimizer, new FieldOptimizer, new PredicateOptimizer,
    new GeneralRegressionModelOptimizer, new NaiveBayesModelOptimizer, new RegressionModelOptimizer)
  def optimize(pmml: PMML) = this.synchronized {
    optimizers.foreach(opt =>
      try {
        opt.applyTo(pmml)
      } catch {
        case t: Throwable => {
          println(s"Error optimizing model for optimizer $opt")
          t.printStackTrace()
          throw t
        }
      })
  }
  private val names = Map(
    "fixed acidity" -> 0,
    "volatile acidity" -> 1, "citric acid" -> 2, "residual sugar" -> 3,
    "chlorides" -> 4, "free sulfur dioxide" -> 5, "total sulfur dioxide" -> 6,
    "density" -> 7, "pH" -> 8, "sulphates" -> 9, "alcohol" -> 10
  )

  override def create(input: ModelToServe): Model = {
      new PMMLModel(input.model)
  }

  override def restore(bytes: Array[Byte]): Model = new PMMLModel(bytes)
}