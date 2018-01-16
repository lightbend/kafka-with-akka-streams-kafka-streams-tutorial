package com.lightbend.scala.custom.store

import com.lightbend.modelServer.model.ModelToServeStats

trait ReadableModelStateStore {
  def getCurrentServingInfo: ModelToServeStats
}
