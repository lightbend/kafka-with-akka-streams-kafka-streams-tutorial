package com.lightbend.scala.modelServer.modelServer

import com.lightbend.modelServer.model.ModelToServeStats

/**
 * Created by boris on 7/21/17.
 */
trait ReadableModelStateStore {
  def getCurrentServingInfo: ModelToServeStats
}

