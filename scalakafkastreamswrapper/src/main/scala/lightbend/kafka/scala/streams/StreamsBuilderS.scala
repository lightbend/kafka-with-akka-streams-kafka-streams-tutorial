package lightbend.kafka.scala.streams

// based on https://github.com/aseigneurin/kafka-streams-scala/blob/master/src/main/scala/com/github/aseigneurin/kafka/streams/scala/KStreamBuilderS.scala

import java.util.regex.Pattern

import lightbend.kafka.scala.streams.ImplicitConversions._
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.GlobalKTable
import org.apache.kafka.streams.processor.{ProcessorSupplier, StateStore}
import org.apache.kafka.streams.state.StoreBuilder
import org.apache.kafka.streams.{Consumed, StreamsBuilder, Topology}

import scala.collection.JavaConversions._

class StreamsBuilderS {

  val inner = new StreamsBuilder

  def streamS[K, V](topics: String*) : KStreamS[K, V] = {
     inner.stream[K, V](seqAsJavaList(topics))
  }

  def streamS[K, V](offsetReset: Topology.AutoOffsetReset,
                   topics: String*) : KStreamS[K, V] =
    inner.stream[K, V](seqAsJavaList(topics), Consumed.`with`[K,V](offsetReset))

  def streamS[K, V](topicPattern: Pattern) : KStreamS[K, V] =
    inner.stream[K, V](topicPattern)

  def streamS[K, V](offsetReset: Topology.AutoOffsetReset, topicPattern: Pattern): KStreamS[K, V] =
    inner.stream[K, V](topicPattern, Consumed.`with`[K,V](offsetReset))

  def table[K, V](topic: String) : KTableS[K, V] = inner.table[K, V](topic)

  def table[K, V](offsetReset: Topology.AutoOffsetReset,
                  topic: String) : KTableS[K, V] =
    inner.table[K, V](topic,  Consumed.`with`[K,V](offsetReset))


  def globalTable[K, V](topic: String): GlobalKTable[K, V] =
    inner.globalTable(topic)

  def addStateStore(builder: StoreBuilder[_ <: StateStore]): StreamsBuilder = inner.addStateStore(builder)

  def addGlobalStore(storeBuilder: StoreBuilder[_ <: StateStore], topic: String, sourceName: String, consumed: Consumed[_, _], processorName: String, stateUpdateSupplier: ProcessorSupplier[_, _]): StreamsBuilder =
    inner.addGlobalStore(storeBuilder,topic,sourceName,consumed,processorName,stateUpdateSupplier)


  def build() : Topology = inner.build()
}
