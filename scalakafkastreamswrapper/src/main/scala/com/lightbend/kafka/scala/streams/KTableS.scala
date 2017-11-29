package com.lightbend.kafka.scala.streams

import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.state.KeyValueStore
import ImplicitConversions._

class KTableS[K, V](val inner: KTable[K, V]) {

  def filter(predicate: (K, V) => Boolean): KTableS[K, V] = {
    inner.filter(predicate(_, _))
  }

  def filter(predicate: (K, V) => Boolean,
    materialized: Materialized[K, V, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, V] = {
    inner.filter(predicate(_, _), materialized)
  }

  def filterNot(predicate: (K, V) => Boolean): KTableS[K, V] = {
    inner.filterNot(predicate(_, _))
  }

  def filterNot(predicate: (K, V) => Boolean,
    materialized: Materialized[K, V, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, V] = {
    inner.filterNot(predicate(_, _), materialized)
  }

  def mapValues[VR](mapper: V => VR): KTableS[K, VR] = {
    def mapperJ: ValueMapper[V, VR] = (v) => mapper(v)
    inner.mapValues[VR](mapperJ)
  }

  def mapValues[VR](mapper: V => VR,
    materialized: Materialized[K, VR, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, VR] = {
    def mapperJ: ValueMapper[V, VR] = (v) => mapper(v)
    inner.mapValues[VR](mapperJ, materialized)
  }

  def toStream: KStreamS[K, V] =
    inner.toStream

  def toStream[KR](mapper: (K, V) => KR): KStreamS[KR, V] = {
    val mapperJ: KeyValueMapper[K, V, KR] = new KeyValueMapper[K, V, KR]{
      override def apply(k: K, v: V): KR = mapper(k, v)
    }
    inner.toStream[KR](mapperJ)
  }

  def groupBy[KR, VR](selector: (K, V) => (KR, VR)): KGroupedTableS[KR, VR] = {
    val selectorJ: KeyValueMapper[K, V, KeyValue[KR, VR]] = (k, v) => {
      val res = selector(k, v)
      new KeyValue[KR, VR](res._1, res._2)
    }
    inner.groupBy(selectorJ)
  }

  def groupBy[KR, VR](selector: (K, V) => (KR, VR),
    serialized: Serialized[KR, VR]): KGroupedTableS[KR, VR] = {

    val selectorJ: KeyValueMapper[K, V, KeyValue[KR, VR]] = (k, v) => {
      val res = selector(k, v)
      new KeyValue[KR, VR](res._1, res._2)
    }
    inner.groupBy(selectorJ, serialized)
  }

  def join[VO, VR](other: KTableS[K, VO],
    joiner: (V, VO) => VR): KTableS[K, VR] = {

    val joinerJ: ValueJoiner[V, VO, VR] = (v1, v2) => joiner(v1, v2)
    inner.join[VO, VR](other.inner, joinerJ)
  }

  def join[VO, VR](other: KTableS[K, VO],
    joiner: (V, VO) => VR,
    materialized: Materialized[K, VR, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, VR] = {

    val joinerJ: ValueJoiner[V, VO, VR] = (v1, v2) => joiner(v1, v2)
    inner.join[VO, VR](other.inner, joinerJ, materialized)
  }

  def leftJoin[VO, VR](other: KTableS[K, VO],
    joiner: (V, VO) => VR): KTableS[K, VR] = {

    val joinerJ: ValueJoiner[V, VO, VR] = (v1, v2) => joiner(v1, v2)
    inner.leftJoin[VO, VR](other.inner, joinerJ)
  }

  def leftJoin[VO, VR](other: KTableS[K, VO],
    joiner: (V, VO) => VR,
    materialized: Materialized[K, VR, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, VR] = {

    val joinerJ: ValueJoiner[V, VO, VR] = (v1, v2) => joiner(v1, v2)
    inner.leftJoin[VO, VR](other.inner, joinerJ, materialized)
  }

  def outerJoin[VO, VR](other: KTableS[K, VO],
    joiner: (V, VO) => VR): KTableS[K, VR] = {

    val joinerJ: ValueJoiner[V, VO, VR] = (v1, v2) => joiner(v1, v2)
    inner.outerJoin[VO, VR](other.inner, joinerJ)
  }

  def outerJoin[VO, VR](other: KTableS[K, VO],
    joiner: (V, VO) => VR,
    materialized: Materialized[K, VR, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, VR] = {

    val joinerJ: ValueJoiner[V, VO, VR] = (v1, v2) => joiner(v1, v2)
    inner.outerJoin[VO, VR](other.inner, joinerJ, materialized)
  }

  def queryableStoreName: String =
    inner.queryableStoreName
}
