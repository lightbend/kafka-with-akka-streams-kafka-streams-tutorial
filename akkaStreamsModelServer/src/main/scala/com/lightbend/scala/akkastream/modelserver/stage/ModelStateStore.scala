package com.lightbend.scala.akkastream.modelserver.stage

import com.lightbend.scala.modelServer.model.{ModelToServeStats, ModelWithDescriptor}

/* Created by boris on 7/21/17. */

/**
 * Abstraction for model information that is persisted and can be restored from storage.
 */
trait ModelStateStore {
  def getCurrentServingInfo: ModelToServeStats

  def setModel(model: ModelWithDescriptor): Unit
}

