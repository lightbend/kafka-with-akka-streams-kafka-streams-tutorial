package com.lightbend.scala.custom.store

import com.lightbend.scala.modelServer.model.ModelToServeStats

trait ReadableModelStateStore {
  def getCurrentServingInfo: ModelToServeStats
}
