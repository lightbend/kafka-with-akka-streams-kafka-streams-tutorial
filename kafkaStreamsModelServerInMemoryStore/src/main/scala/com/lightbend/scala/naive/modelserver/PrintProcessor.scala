package com.lightbend.scala.naive.modelserver

import com.lightbend.scala.modelServer.model.{ModelToServe, ModelToServeStats, ModelWithDescriptor, ServingResult}
import com.lightbend.scala.naive.modelserver.store.StoreState
import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext}

import scala.util.Success

class PrintProcessor extends AbstractProcessor[Array[Byte], ServingResult]{

  private var modelStore: StoreState = null

  override def process (key: Array[Byte], value: ServingResult ): Unit = {
    // Exercise: Provide implementation here.
    // 1. Match on `value.processed`, which returns a Boolean
    // 2. If true, print fields in the value object
    // 1. If false, print that no model is available, so skipping...
  }
}
