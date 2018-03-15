package com.lightbend.scala.kafkastreams.store.store

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import java.util

import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.scala.modelServer.model.PMML.PMMLModel
import com.lightbend.scala.modelServer.model.tensorflow.TensorFlowModel
import com.lightbend.scala.modelServer.model.{ModelToServeStats, ModelWithDescriptor}
import com.lightbend.scala.kafkastreams.store.StoreState
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

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): StoreState = {
    if(data != null) {
      val input = new DataInputStream(new ByteArrayInputStream(data))
      new StoreState(ModelWithDescriptor.readModel(input), ModelWithDescriptor.readModel(input),
        ModelToServeStats.readServingInfo(input), ModelToServeStats.readServingInfo(input))
    }
    else new StoreState()
  }

  override def close(): Unit = {}

}

class ModelStateSerializer extends Serializer[StoreState] {

  private val bos = new ByteArrayOutputStream()

  override def serialize(topic: String, state: StoreState): Array[Byte] = {
    bos.reset()
    val output = new DataOutputStream(bos)
    ModelWithDescriptor.writeModel(output, state.currentModel.orNull)
    ModelWithDescriptor.writeModel(output, state.newModel.orNull)
    ModelToServeStats.writeServingInfo(output, state.currentState.orNull)
    ModelToServeStats.writeServingInfo(output, state.newState.orNull)
    try {
      output.flush()
      output.close()
    } catch {
      case t: Throwable =>
    }
    bos.toByteArray
  }

  override def close(): Unit = {}

  override def configure(configs: util.Map[String, _], isKey: Boolean) = {}
}
