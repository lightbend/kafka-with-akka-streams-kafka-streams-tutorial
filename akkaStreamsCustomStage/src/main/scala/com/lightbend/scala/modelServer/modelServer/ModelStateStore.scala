package com.lightbend.scala.modelServer.modelServer

import com.lightbend.scala.modelServer.model.ModelToServeStats
import com.lightbend.scala.modelServer.model.ModelWithDescriptor

/**
 * Created by boris on 7/21/17.
 */
trait ModelStateStore {
  def getCurrentServingInfo: ModelToServeStats

  def setModel(model: ModelWithDescriptor): Unit
}

