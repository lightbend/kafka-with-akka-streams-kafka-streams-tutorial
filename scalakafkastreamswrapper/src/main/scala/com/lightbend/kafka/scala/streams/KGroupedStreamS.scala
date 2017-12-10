package com.lightbend.kafka.scala.streams

import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.common.serialization.Serde
import ImplicitConversions._
import FunctionConversions._


class KGroupedStreamS[K, V](inner: KGroupedStream[K, V]) {

  def count(): KTableS[K, Long] = {
    val c: KTableS[K, java.lang.Long] = inner.count()
    c.mapValues[Long](Long2long _)
  }

  def count(store: String, keySerde: Option[Serde[K]] = None): KTableS[K, Long] = {
    val materialized = keySerde.foldLeft(Materialized.as[K, java.lang.Long, KeyValueStore[Bytes, Array[Byte]]](store))((m,serde)=> m.withKeySerde(serde))

    val c: KTableS[K, java.lang.Long] = inner.count(materialized)
    c.mapValues[Long](Long2long _)
  }

  def reduce(reducer: (V, V) => V): KTableS[K, V] = {
    inner.reduce((v1, v2) => reducer(v1, v2))
  }

  def reduce(reducer: (V, V) => V,
    materialized: Materialized[K, V, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, V] = {

    inner.reduce((v1: V, v2: V) => reducer(v1, v2), materialized)
  }

  def reduce(reducer: (V, V) => V,
    storeName: String): KTableS[K, V] = {

    inner.reduce((v1: V, v2: V) => reducer(v1, v2), Materialized.as[K, V, KeyValueStore[Bytes, Array[Byte]]](storeName))
  }

  def aggregate[VR](initializer: () => VR,
    aggregator: (K, V, VR) => VR): KTableS[K, VR] = {
    inner.aggregate(initializer.asInitializer, aggregator.asAggregator)
  }

  def aggregate[VR](initializer: () => VR,
    aggregator: (K, V, VR) => VR,
    materialized: Materialized[K, VR, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, VR] = {
    inner.aggregate(initializer.asInitializer, aggregator.asAggregator, materialized)
  }

  def windowedBy(windows: SessionWindows): SessionWindowedKStreamS[K, V] =
    inner.windowedBy(windows)

  def windowedBy[W <: Window](windows: Windows[W]): TimeWindowedKStreamS[K, V] =
    inner.windowedBy(windows)
}
