package com.lightbend.scala.akkastream.modelserver.stage

import com.lightbend.scala.modelServer.model.{ModelToServeStats, ModelWithDescriptor}

/**
 * Created by boris on 7/21/17.
 */
trait ModelStateStore {
  def getCurrentServingInfo: ModelToServeStats

  def setModel(model: ModelWithDescriptor): Unit
}

