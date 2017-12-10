package com.lightbend.kafka.scala.streams

import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.state.SessionStore
import org.apache.kafka.common.utils.Bytes
import FunctionConversions._

import ImplicitConversions._

class SessionWindowedKStreamS[K, V](val inner: SessionWindowedKStream[K, V]) {

  def aggregate[VR](initializer: () => VR,
    aggregator: (K, V, VR) => VR,
    merger: (K, VR, VR) => VR): KTableS[Windowed[K], VR] = {

    inner.aggregate(initializer.asInitializer, aggregator.asAggregator, merger.asMerger)
  }

  def aggregate[VR](initializer: () => VR,
    aggregator: (K, V, VR) => VR,
    merger: (K, VR, VR) => VR,
    materialized: Materialized[K, VR, SessionStore[Bytes, Array[Byte]]]): KTableS[Windowed[K], VR] = {

    inner.aggregate(initializer.asInitializer, aggregator.asAggregator, merger.asMerger, materialized)
  }

  def count(): KTableS[Windowed[K], Long] = {
    val c: KTableS[Windowed[K], java.lang.Long] = inner.count()
    c.mapValues[Long](Long2long(_))
  }

  def count(materialized: Materialized[K, Long, SessionStore[Bytes, Array[Byte]]]): KTableS[Windowed[K], Long] = 
    inner.count(materialized)

  def reduce(reducer: (V, V) => V): KTableS[Windowed[K], V] = {
    inner.reduce((v1, v2) => reducer(v1, v2))
  }

  def reduce(reducer: (V, V) => V,
    materialized: Materialized[K, V, SessionStore[Bytes, Array[Byte]]]): KTableS[Windowed[K], V] = {

    val reducerJ: Reducer[V] = (v1: V, v2: V) => reducer(v1, v2)
    inner.reduce(reducer.asReducer, materialized)
  }
}
