package com.lightbend.scala.naive.modelserver

import com.lightbend.scala.modelServer.model.{ModelToServe, ModelToServeStats, ModelWithDescriptor, ServingResult}
import com.lightbend.scala.naive.modelserver.store.StoreState
import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext}

import scala.util.Success

class PrintProcessor extends AbstractProcessor[Array[Byte], ServingResult]{

  private var modelStore: StoreState = null

  override def process (key: Array[Byte], value: ServingResult ): Unit = ???

  /* Provide an implementation here
   */
}
