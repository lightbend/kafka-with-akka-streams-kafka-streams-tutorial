package com.lightbend.kafka.scala.streams

import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.state.WindowStore
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.common.serialization.Serde
import ImplicitConversions._
import FunctionConversions._

class TimeWindowedKStreamS[K, V](val inner: TimeWindowedKStream[K, V]) {

  def aggregate[VR](initializer: () => VR,
    aggregator: (K, V, VR) => VR): KTableS[Windowed[K], VR] = {

    inner.aggregate(initializer.asInitializer, aggregator.asAggregator)
  }

  def aggregate[VR](initializer: () => VR,
    aggregator: (K, V, VR) => VR,
    materialized: Materialized[K, VR, WindowStore[Bytes, Array[Byte]]]): KTableS[Windowed[K], VR] = {

    inner.aggregate(initializer.asInitializer, aggregator.asAggregator, materialized)
  }

  def count(): KTableS[Windowed[K], Long] = {
    val c: KTableS[Windowed[K], java.lang.Long] = inner.count()
    c.mapValues[Long](Long2long(_))
  }

  def count(store: String, keySerde: Option[Serde[K]] = None): KTableS[Windowed[K], Long] = {
    val materialized = {
      val m = Materialized.as[K, java.lang.Long, WindowStore[Bytes, Array[Byte]]](store)
      keySerde.foldLeft(m)((m,serde)=> m.withKeySerde(serde))
    }
    val c: KTableS[Windowed[K], java.lang.Long] = inner.count(materialized)
    c.mapValues[Long](Long2long(_))
  }

  def reduce(reducer: (V, V) => V): KTableS[Windowed[K], V] = {
    inner.reduce(reducer.asReducer)
  }

  def reduce(reducer: (V, V) => V,
    materialized: Materialized[K, V, WindowStore[Bytes, Array[Byte]]]): KTableS[Windowed[K], V] = {

    inner.reduce(reducer.asReducer, materialized)
  }
}
