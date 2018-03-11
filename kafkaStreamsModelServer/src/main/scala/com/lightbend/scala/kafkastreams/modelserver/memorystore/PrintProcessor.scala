package com.lightbend.scala.kafkastreams.modelserver.memorystore

import com.lightbend.scala.modelServer.model.ServingResult
import org.apache.kafka.streams.processor.AbstractProcessor

class PrintProcessor extends AbstractProcessor[Array[Byte], ServingResult]{

  override def process (key: Array[Byte], value: ServingResult ): Unit = {
    value.processed match {
      case true => println(s"Calculated quality - ${value.result} calculated in ${value.duration} ms")
      case _ => println("No model available - skipping")
    }
  }
}
