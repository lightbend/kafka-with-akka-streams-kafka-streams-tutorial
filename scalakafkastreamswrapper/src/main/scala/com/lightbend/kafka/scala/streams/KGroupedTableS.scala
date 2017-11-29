package com.lightbend.kafka.scala.streams

import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.state.KeyValueStore
import ImplicitConversions._

class KGroupedTableS[K, V](inner: KGroupedTable[K, V]) {

  def count(): KTableS[K, Long] = {
    val c: KTableS[K, java.lang.Long] = inner.count()
    c.mapValues[Long](Long2long(_))
  }

  def count(materialized: Materialized[K, Long, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, Long] = 
    inner.count(materialized)

  def reduce(adder: (V, V) => V,
    subtractor: (V, V) => V): KTableS[K, V] = {

    val adderJ: Reducer[V] = (v1: V, v2: V) => adder(v1, v2)
    val subtractorJ: Reducer[V] = (v1: V, v2: V) => subtractor(v1, v2)
    inner.reduce(adderJ, subtractorJ)
  }

  def reduce(adder: (V, V) => V,
    subtractor: (V, V) => V,
    materialized: Materialized[K, V, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, V] = {

    val adderJ: Reducer[V] = (v1: V, v2: V) => adder(v1, v2)
    val subtractorJ: Reducer[V] = (v1: V, v2: V) => subtractor(v1, v2)
    inner.reduce(adderJ, subtractorJ, materialized)
  }

  def aggregate[VR](initializer: () => VR,
    adder: (K, V, VR) => VR,
    subtractor: (K, V, VR) => VR): KTableS[K, VR] = {

    val initializerJ: Initializer[VR] = () => initializer()
    val adderJ: Aggregator[K, V, VR] = (k: K, v: V, va: VR) => adder(k, v, va)
    val subtractorJ: Aggregator[K, V, VR] = (k: K, v: V, va: VR) => subtractor(k, v, va)
    inner.aggregate(initializerJ, adderJ, subtractorJ)
  }

  def aggregate[VR](initializer: () => VR,
    adder: (K, V, VR) => VR,
    subtractor: (K, V, VR) => VR,
    materialized: Materialized[K, VR, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, VR] = {

    val initializerJ: Initializer[VR] = () => initializer()
    val adderJ: Aggregator[K, V, VR] = (k: K, v: V, va: VR) => adder(k, v, va)
    val subtractorJ: Aggregator[K, V, VR] = (k: K, v: V, va: VR) => subtractor(k, v, va)
    inner.aggregate(initializerJ, adderJ, subtractorJ, materialized)
  }
}
