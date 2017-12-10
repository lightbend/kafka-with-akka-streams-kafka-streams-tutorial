package com.lightbend.kafka.scala.streams

import java.util.regex.Pattern

import ImplicitConversions._
import org.apache.kafka.streams.kstream.{ GlobalKTable, Materialized }
import org.apache.kafka.streams.processor.{ProcessorSupplier, StateStore}
import org.apache.kafka.streams.state.{ StoreBuilder, KeyValueStore }
import org.apache.kafka.streams.{Consumed, StreamsBuilder, Topology}
import org.apache.kafka.common.utils.Bytes

import scala.collection.JavaConverters._

class StreamsBuilderS {

  val inner = new StreamsBuilder

  def stream[K, V](topic: String) : KStreamS[K, V] =
     inner.stream[K, V](topic)

  def stream[K, V](topic: String, consumed: Consumed[K, V]) : KStreamS[K, V] = 
     inner.stream[K, V](topic, consumed)

  def stream[K, V](topics: List[String]): KStreamS[K, V] = 
     inner.stream[K, V](topics.asJava)

  def stream[K, V](topics: List[String], consumed: Consumed[K, V]): KStreamS[K, V] =
     inner.stream[K, V](topics.asJava, consumed)

  def stream[K, V](topicPattern: Pattern) : KStreamS[K, V] =
    inner.stream[K, V](topicPattern)

  def stream[K, V](topicPattern: Pattern, consumed: Consumed[K, V]) : KStreamS[K, V] =
    inner.stream[K, V](topicPattern, consumed)

  def table[K, V](topic: String) : KTableS[K, V] = inner.table[K, V](topic)

  def table[K, V](topic: String, consumed: Consumed[K, V]) : KTableS[K, V] =
    inner.table[K, V](topic, consumed)

  def table[K, V](topic: String, consumed: Consumed[K, V],
    materialized: Materialized[K, V, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, V] = 
    inner.table[K, V](topic,  consumed, materialized)

  def table[K, V](topic: String, 
    materialized: Materialized[K, V, KeyValueStore[Bytes, Array[Byte]]]): KTableS[K, V] = 
    inner.table[K, V](topic, materialized)

  def globalTable[K, V](topic: String): GlobalKTable[K, V] =
    inner.globalTable(topic)

  def addStateStore(builder: StoreBuilder[_ <: StateStore]): StreamsBuilder = inner.addStateStore(builder)

  def addGlobalStore(storeBuilder: StoreBuilder[_ <: StateStore], topic: String, sourceName: String, consumed: Consumed[_, _], processorName: String, stateUpdateSupplier: ProcessorSupplier[_, _]): StreamsBuilder =
    inner.addGlobalStore(storeBuilder,topic,sourceName,consumed,processorName,stateUpdateSupplier)

  def build() : Topology = inner.build()
}


