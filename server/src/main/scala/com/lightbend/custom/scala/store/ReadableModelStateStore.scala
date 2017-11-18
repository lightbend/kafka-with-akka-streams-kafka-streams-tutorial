package com.lightbend.custom.scala.store

import com.lightbend.modelServer.model.ModelToServeStats

trait ReadableModelStateStore {
  def getCurrentServingInfo: ModelToServeStats
}
