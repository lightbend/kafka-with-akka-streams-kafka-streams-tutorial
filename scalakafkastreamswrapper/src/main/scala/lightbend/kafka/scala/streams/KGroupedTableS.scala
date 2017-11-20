package lightbend.kafka.scala.streams

// Based on https://github.com/aseigneurin/kafka-streams-scala/blob/master/src/main/scala/com/github/aseigneurin/kafka/streams/scala/KGroupedTableS.scala

import lightbend.kafka.scala.streams.ImplicitConversions._
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.state.{KeyValueStore, StoreSupplier}

class KGroupedTableS[K, V](inner: KGroupedTable[K, V]) {
/*
  def count(storeName: String): KTableS[K, Long] = {
    inner.count(storeName)
      .mapValues[Long](javaLong => Long.box(javaLong))
  }

  def count(storeSupplier: StateStoreSupplier[KeyValueStore[_, _]]): KTableS[K, Long] = {
    inner.count(storeSupplier)
      .mapValues[Long](javaLong => Long.box(javaLong))
  }
*/
  def reduce(adder: (V, V) => V,
             subtractor: (V, V) => V,
             storeName: String): KTableS[K, V] = {
    val adderJ: Reducer[V] = new Reducer[V]{
      override def apply(v1: V, v2: V): V = adder(v1, v2)
    }
    val subtractorJ: Reducer[V] = new Reducer[V]{
      override def apply(v1: V, v2: V): V = subtractor(v1, v2)
    }
    inner.reduce(adderJ, subtractorJ, storeName)
  }

  def reduce(adder: Reducer[V],
             subtractor: Reducer[V],
             storeSupplier: StoreSupplier[KeyValueStore[_, _]]): KTableS[K, V] = {
    val adderJ: Reducer[V] = new Reducer[V]{
      override def apply(v1: V, v2: V): V = adder(v1, v2)
    }
    val subtractorJ: Reducer[V] = new Reducer[V]{
      override def apply(v1: V, v2: V): V = subtractor(v1, v2)
    }
    inner.reduce(adderJ, subtractorJ, storeSupplier)
  }

  def aggregate[VR](initializer: () => VR,
                    adder: (K, V, VR) => VR,
                    subtractor: (K, V, VR) => VR,
                    storeName: String): KTableS[K, VR] = {
    val initializerJ: Initializer[VR] = new Initializer[VR]{
      override def apply(): VR = initializer()
    }
    val adderJ: Aggregator[K, V, VR] = new Aggregator[K, V, VR]{
      override def apply(key: K, value: V, aggregate: VR): VR = adder(key, value, aggregate)
    }
    val subtractorJ: Aggregator[K, V, VR] = new Aggregator[K, V, VR]{
      override def apply(key: K, value: V, aggregate: VR): VR = subtractor(key, value, aggregate)
    }
    inner.aggregate(initializerJ, adderJ, subtractorJ, storeName)
  }

  def aggregate[VR](initializer: () => VR,
                    adder: (K, V, VR) => VR,
                    subtractor: (K, V, VR) => VR,
                    aggValueSerde: Serde[VR],
                    storeName: String): KTableS[K, VR] = {
    val initializerJ: Initializer[VR] = new Initializer[VR]{
      override def apply(): VR = initializer()
    }
    val adderJ: Aggregator[K, V, VR] = new Aggregator[K, V, VR]{
      override def apply(key: K, value: V, aggregate: VR): VR = adder(key, value, aggregate)
    }
    val subtractorJ: Aggregator[K, V, VR] = new Aggregator[K, V, VR]{
      override def apply(key: K, value: V, aggregate: VR): VR = subtractor(key, value, aggregate)
    }
    inner.aggregate(initializerJ, adderJ, subtractorJ, aggValueSerde, storeName)
  }
/*
  def aggregate[VR](initializer: () => VR,
                    adder: (K, V, VR) => VR,
                    subtractor: (K, V, VR) => VR,
                    storeSupplier: StoreSupplier[KeyValueStore[_, _]]): KTableS[K, VR] = {
    val initializerJ: Initializer[VR] = () => initializer()
    val adderJ: Aggregator[K, V, VR] = (k: K, v: V, va: VR) => adder(k, v, va)
    val subtractorJ: Aggregator[K, V, VR] = (k: K, v: V, va: VR) => subtractor(k, v, va)
    inner.aggregate(initializerJ, adderJ, subtractorJ, storeSupplier)
  } */
}