package com.lightbend.scala.kafkastreams.store.store.custom

import com.lightbend.scala.modelServer.model.ModelToServeStats

trait ReadableModelStateStore {
  def getCurrentServingInfo: ModelToServeStats
}
