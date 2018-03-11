package com.lightbend.scala.kafkastreams.store.store.custom

import java.util
import java.util.{Objects}

import org.apache.kafka.streams.state.StoreBuilder

class ModelStateStoreBuilder(nm: String) extends StoreBuilder [ModelStateStore]{
  Objects.requireNonNull(nm, "name can't be null")

  var lConfig: util.Map[String, String] = new util.HashMap[String, String]
  var enableCaching: Boolean = false
  var enableLogging: Boolean = true

  override def build : ModelStateStore = new ModelStateStore(nm, enableLogging)

  override def withCachingEnabled: ModelStateStoreBuilder = {
    enableCaching = true
    this
  }

  override def withLoggingEnabled(config: util.Map[String, String]): ModelStateStoreBuilder = {
    Objects.requireNonNull(config, "config can't be null")
    enableLogging = true
    lConfig = config
    this
  }

  override def withLoggingDisabled: ModelStateStoreBuilder = {
    enableLogging = false
    lConfig.clear()
    this
  }

  override def logConfig: util.Map[String, String] = lConfig

  override def loggingEnabled: Boolean = enableLogging

  override def name: String = nm
}