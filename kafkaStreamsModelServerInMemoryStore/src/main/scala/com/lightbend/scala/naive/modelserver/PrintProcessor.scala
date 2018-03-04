package com.lightbend.scala.naive.modelserver

import com.lightbend.scala.modelServer.model.ServingResult
import com.lightbend.scala.naive.modelserver.store.StoreState
import org.apache.kafka.streams.processor.AbstractProcessor


class PrintProcessor extends AbstractProcessor[Array[Byte], ServingResult]{

  private var modelStore: StoreState = null

  override def process (key: Array[Byte], value: ServingResult ): Unit = {
    value.processed match {
      case true => println(s"Calculated quality - ${value.result} calculated in ${value.duration} ms")
      case _ => println("No model available - skipping")
    }
  }
}
