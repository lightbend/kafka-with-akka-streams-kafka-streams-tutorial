package com.lightbend.kafka.scala.streams

import ImplicitConversions._
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.common.utils.Bytes
import FunctionConversions._

class KGroupedTableS[K, V](inner: KGroupedTable[K, V]) {

  type ByteArrayKVStore = KeyValueStore[Bytes, Array[Byte]]

  def count(): KTableS[K, Long] = {
    val c: KTableS[K, java.lang.Long] = inner.count()
    c.mapValues[Long](Long2long(_))
  }

  def count(materialized: Materialized[K, Long, ByteArrayKVStore]): KTableS[K, Long] =
    inner.count(materialized)

  def reduce(adder: (V, V) => V,
    subtractor: (V, V) => V): KTableS[K, V] = {

    inner.reduce((v1, v2) => adder(v1, v2), (v1, v2) => subtractor(v1, v2))
  }

  def reduce(adder: (V, V) => V,
    subtractor: (V, V) => V,
    materialized: Materialized[K, V, ByteArrayKVStore]): KTableS[K, V] = {

    inner.reduce((v1, v2) => adder(v1, v2), (v1, v2) => subtractor(v1, v2), materialized)
  }

  def aggregate[VR](initializer: () => VR,
    adder: (K, V, VR) => VR,
    subtractor: (K, V, VR) => VR): KTableS[K, VR] = {

    inner.aggregate(initializer.asInitializer, adder.asAggregator, subtractor.asAggregator)
  }

  def aggregate[VR](initializer: () => VR,
    adder: (K, V, VR) => VR,
    subtractor: (K, V, VR) => VR,
    materialized: Materialized[K, VR, ByteArrayKVStore]): KTableS[K, VR] = {


    inner.aggregate(initializer.asInitializer, adder.asAggregator, subtractor.asAggregator, materialized)
  }
}
