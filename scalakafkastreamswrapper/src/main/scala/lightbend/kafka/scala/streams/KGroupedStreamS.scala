package lightbend.kafka.scala.streams

// based on https://github.com/aseigneurin/kafka-streams-scala/blob/master/src/main/scala/com/github/aseigneurin/kafka/streams/scala/KGroupedStreamS.scala

import lightbend.kafka.scala.streams.ImplicitConversions._
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.processor.StateStoreSupplier
import org.apache.kafka.streams.state.{KeyValueStore, SessionStore, WindowStore}

class KGroupedStreamS[K, V](inner: KGroupedStream[K, V]) {

/*
  def count(storeName: String): KTableS[K, Long] = {
    inner.count(storeName)
      .mapValues[Long](javaLong => Long.box(javaLong))
  }

  def count(storeSupplier: StateStoreSupplier[KeyValueStore[_, _]]): KTableS[K, Long] = {
    inner.count(storeSupplier)
      .mapValues[Long](javaLong => Long.box(javaLong))
  }

  def count[W <: Window](windows: Windows[W],
                         storeName: String): KTableS[Windowed[K], Long] = {
    inner.count[W](windows, storeName)
      .mapValues[Long](javaLong => Long.box(javaLong))
  }

  def count[W <: Window](windows: Windows[W],
                         storeSupplier: StateStoreSupplier[WindowStore[_, _]]): KTableS[Windowed[K], Long] = {
    inner.count[W](windows, storeSupplier)
      .mapValues[Long](javaLong => Long.box(javaLong))
  }

  def count(sessionWindows: SessionWindows, storeName: String): KTableS[Windowed[K], Long] = {
    inner.count(sessionWindows, storeName)
      .mapValues[Long](javaLong => Long.box(javaLong))
  }

  def count(sessionWindows: SessionWindows,
            storeSupplier: StateStoreSupplier[SessionStore[_, _]]): KTableS[Windowed[K], Long] = {
    inner.count(sessionWindows, storeSupplier)
      .mapValues[Long](javaLong => Long.box(javaLong))
  }
*/
  def reduce(reducer: (V, V) => V,
             storeName: String): KTableS[K, V] = {
    val reducerJ: Reducer[V] = new Reducer[V]{
      override def apply(value1: V, value2: V): V = reducer(value1, value2)
    }
    inner.reduce(reducerJ, storeName)
  }

  def reduce(reducer: (V, V) => V,
             storeSupplier: StateStoreSupplier[KeyValueStore[_, _]]): KTableS[K, V] = {
    val reducerJ: Reducer[V] = new Reducer[V]{
      override def apply(value1: V, value2: V): V = reducer(value1, value2)
    }
    inner.reduce(reducerJ, storeSupplier)
  }

  def reduce[W <: Window](reducer: (V, V) => V,
                          windows: Windows[W],
                          storeName: String): KTableS[Windowed[K], V] = {
    val reducerJ: Reducer[V] = new Reducer[V]{
      override def apply(value1: V, value2: V): V = reducer(value1, value2)
    }
    inner.reduce(reducerJ, windows, storeName)
  }

  def reduce[W <: Window](reducer: (V, V) => V,
                          windows: Windows[W],
                          storeSupplier: StateStoreSupplier[WindowStore[_, _]]): KTableS[Windowed[K], V] = {
    val reducerJ: Reducer[V] = new Reducer[V]{
      override def apply(value1: V, value2: V): V = reducer(value1, value2)
    }
    inner.reduce(reducerJ, windows, storeSupplier)
  }

  def reduce(reducer: (V, V) => V,
             sessionWindows: SessionWindows,
             storeName: String): KTableS[Windowed[K], V] = {
    val reducerJ: Reducer[V] = new Reducer[V]{
      override def apply(value1: V, value2: V): V = reducer(value1, value2)
    }
    inner.reduce(reducerJ, sessionWindows, storeName)
  }

  def reduce(reducer: (V, V) => V,
             sessionWindows: SessionWindows,
             storeSupplier: StateStoreSupplier[SessionStore[_, _]]): KTableS[Windowed[K], V] = {
    val reducerJ: Reducer[V] = new Reducer[V]{
      override def apply(value1: V, value2: V): V = reducer(value1, value2)
    }
    inner.reduce(reducerJ, sessionWindows, storeSupplier)
  }

  def aggregate[VR](initializer: () => VR,
                    aggregator: (K, V, VR) => VR,
                    aggValueSerde: Serde[VR],
                    storeName: String): KTableS[K, VR] = {
    val initializerJ: Initializer[VR] = new Initializer[VR]{
      override def apply(): VR = initializer()
    }
    val aggregatorJ: Aggregator[K, V, VR] = new Aggregator[K, V, VR]{
      override def apply(key: K, value: V, aggregate: VR): VR = aggregator(key, value, aggregate)
    }
    inner.aggregate(initializerJ, aggregatorJ, aggValueSerde, storeName)
  }

  def aggregate[VR](initializer: () => VR,
                    aggregator: (K, V, VR) => VR,
                    storeSupplier: StateStoreSupplier[KeyValueStore[_, _]]): KTableS[K, VR] = {
    val initializerJ: Initializer[VR] = new Initializer[VR]{
      override def apply(): VR = initializer()
    }
    val aggregatorJ: Aggregator[K, V, VR] = new Aggregator[K, V, VR]{
      override def apply(key: K, value: V, aggregate: VR): VR = aggregator(key, value, aggregate)
    }
    inner.aggregate(initializerJ, aggregatorJ, storeSupplier)
  }

  def aggregate[W <: Window, VR](initializer: () => VR,
                                 aggregator: (K, V, VR) => VR,
                                 windows: Windows[W],
                                 aggValueSerde: Serde[VR],
                                 storeName: String): KTableS[Windowed[K], VR] = {
    val initializerJ: Initializer[VR] = new Initializer[VR]{
      override def apply(): VR = initializer()
    }
    val aggregatorJ: Aggregator[K, V, VR] = new Aggregator[K, V, VR]{
      override def apply(key: K, value: V, aggregate: VR): VR = aggregator(key, value, aggregate)
    }
    inner.aggregate(initializerJ, aggregatorJ, windows, aggValueSerde, storeName)
  }

  def aggregate[W <: Window, VR](initializer: () => VR,
                                 aggregator: (K, V, VR) => VR,
                                 windows: Windows[W],
                                 storeSupplier: StateStoreSupplier[WindowStore[_, _]]): KTableS[Windowed[K], VR] = {
    val initializerJ: Initializer[VR] = new Initializer[VR]{
      override def apply(): VR = initializer()
    }
    val aggregatorJ: Aggregator[K, V, VR] = new Aggregator[K, V, VR]{
      override def apply(key: K, value: V, aggregate: VR): VR = aggregator(key, value, aggregate)
    }
    inner.aggregate(initializerJ, aggregatorJ, windows, storeSupplier)
  }

  def aggregate[T](initializer: Initializer[T],
                   aggregator: (K, V, T) => T,
                   sessionMerger: (K, T, T) => T,
                   sessionWindows: SessionWindows,
                   aggValueSerde: Serde[T],
                   storeName: String): KTableS[Windowed[K], T] = {
    val initializerJ: Initializer[T] = new Initializer[T]{
      override def apply(): T = initializer()
    }
    val aggregatorJ: Aggregator[K, V, T] = new Aggregator[K, V, T]{
      override def apply(key: K, value: V, aggregate: T): T = aggregator(key, value, aggregate)
    }
    val sessionMergerJ: Merger[K, T] = new Merger[K, T]{
      override def apply(aggKey: K, aggOne: T, aggTwo: T): T = sessionMerger(aggKey, aggOne, aggTwo)
    }
    inner.aggregate(initializerJ, aggregatorJ, sessionMergerJ, sessionWindows, aggValueSerde, storeName)
  }

  def aggregate[T](initializer: Initializer[T],
                   aggregator: (K, V, T) => T,
                   sessionMerger: Merger[_ >: K, T],
                   sessionWindows: SessionWindows,
                   aggValueSerde: Serde[T],
                   storeSupplier: StateStoreSupplier[SessionStore[_, _]]): KTableS[Windowed[K], T] = {
    val initializerJ: Initializer[T] = new Initializer[T]{
      override def apply(): T = initializer()
    }
    val aggregatorJ: Aggregator[K, V, T] = new Aggregator[K, V, T]{
      override def apply(key: K, value: V, aggregate: T): T = aggregator(key, value, aggregate)
    }
    val sessionMergerJ: Merger[K, T] = new Merger[K, T]{
      override def apply(aggKey: K, aggOne: T, aggTwo: T): T = sessionMerger(aggKey, aggOne, aggTwo)
    }
    inner.aggregate(initializerJ, aggregatorJ, sessionMergerJ, sessionWindows, aggValueSerde, storeSupplier)
  }

}