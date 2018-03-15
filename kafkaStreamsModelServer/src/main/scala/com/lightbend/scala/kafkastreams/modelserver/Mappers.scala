package com.lightbend.scala.kafkastreams.modelserver

import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.modelServer.model.{DataRecord, ModelToServe, ModelWithDescriptor, ServingResult}
import org.apache.kafka.streams.kstream.{Predicate, ValueMapper}

import scala.util.Try

class DataValueMapper extends ValueMapper[Array[Byte], Try[WineRecord]] {
  override def apply(value: Array[Byte]): Try[WineRecord] = DataRecord.fromByteArray(value)
}

class DataValueFilter extends Predicate[Array[Byte], Try[WineRecord]]{
  override def test(key: Array[Byte], value: Try[WineRecord]): Boolean = value.isSuccess
}

class ModelValueMapper extends ValueMapper[Array[Byte], Try[ModelToServe]] {
  override def apply(value: Array[Byte]): Try[ModelToServe] = ModelToServe.fromByteArray(value)
}

class ModelValueFilter extends Predicate[Array[Byte], Try[ModelToServe]]{
  override def test(key: Array[Byte], value: Try[ModelToServe]): Boolean = value.isSuccess
}

class ModelDescriptorMapper extends ValueMapper[Try[ModelToServe],  Try[ModelWithDescriptor]] {
  override def apply(value: Try[ModelToServe]):  Try[ModelWithDescriptor] = ModelWithDescriptor.fromModelToServe(value.get)
}

class ResultPrinter extends ValueMapper[ServingResult,  ServingResult] {
  override def apply(value: ServingResult):  ServingResult = {
    if(value.processed) println(s"Calculated quality - ${value.result} calculated in ${value.duration} ms")
    else println("No model available - skipping")
    value
  }
}

class ModelDescriptorFilter extends Predicate[Array[Byte], Try[ModelWithDescriptor]]{
  override def test(key: Array[Byte], value: Try[ModelWithDescriptor]): Boolean = value.isSuccess
}

