package lightbend.kafka.scala.streams

// Based on https://github.com/aseigneurin/kafka-streams-scala/blob/master/src/main/scala/com/github/aseigneurin/kafka/streams/scala/ImplicitConversions.scala

import org.apache.kafka.streams.kstream.{KGroupedStream, KGroupedTable, KStream, KTable}

import scala.language.implicitConversions

object ImplicitConversions {

  implicit def wrapKStream[K, V](inner: KStream[K, V]): KStreamS[K, V] =
    new KStreamS[K, V](inner)

  implicit def wrapKGroupedStream[K, V](inner: KGroupedStream[K, V]): KGroupedStreamS[K, V] =
    new KGroupedStreamS[K, V](inner)

  implicit def wrapKTable[K, V](inner: KTable[K, V]): KTableS[K, V] =
    new KTableS[K, V](inner)

  implicit def wrapKGroupedTable[K, V](inner: KGroupedTable[K, V]): KGroupedTableS[K, V] =
    new KGroupedTableS[K, V](inner)

}