package lightbend.kafka.scala.streams

// Based on https://github.com/aseigneurin/kafka-streams-scala/blob/master/src/main/scala/com/github/aseigneurin/kafka/streams/scala/KTableS.scala

import lightbend.kafka.scala.streams.ImplicitConversions._
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.processor.StreamPartitioner

class KTableS[K, V](val inner: KTable[K, V]) {

  def filter(predicate: (K, V) => Boolean): KTableS[K, V] = {
    val predicateJ: Predicate[K, V] = new Predicate[K, V]{
      override def test(k: K, v: V): Boolean = predicate(k, v)
    }
    inner.filter(predicateJ)
  }

  def filterNot(predicate: (K, V) => Boolean): KTableS[K, V] = {
    val predicateJ: Predicate[K, V] = new Predicate[K, V]{
      override def test(key: K, value: V): Boolean = predicate(key, value)
    }
    inner.filterNot(predicateJ)
  }

  def mapValues[VR](mapper: (V) => VR): KTable[K, VR] = {
    def mapperJ: ValueMapper[V, VR] = new ValueMapper[V, VR]{
      override def apply(value: V): VR = mapper(value)
    }
    inner.mapValues(mapperJ)
  }

  def print() = inner.print()

  def print(streamName: String) = inner.print(streamName)

  def print(keySerde: Serde[K], valSerde: Serde[V]) = inner.print(keySerde, valSerde)

  def print(keySerde: Serde[K], valSerde: Serde[V], streamName: String) =
    inner.print(keySerde, valSerde, streamName)

  def writeAsText(filePath: String, keySerde: Serde[K], valSerde: Serde[V]) = {
    inner.writeAsText(filePath, keySerde, valSerde)
  }

  def writeAsText(filePath: String,
                  streamName: String, keySerde: Serde[K], valSerde: Serde[V]) = {
    inner.writeAsText(filePath, streamName, keySerde, valSerde)
  }

  def foreach(action: (K, V) => Unit): Unit = {
    val actionJ: ForeachAction[K, V] = new ForeachAction[K, V]{
      override def apply(key: K, value: V): Unit = action(key, value)
    }
    inner.foreach(actionJ)
  }

  def toStream: KStreamS[K, V] =
    inner.toStream

  def toStream[KR](mapper: (K, V) => KR): KStreamS[KR, V] = {
    val mapperJ: KeyValueMapper[K, V, KR] = new KeyValueMapper[K, V, KR]{
      override def apply(k: K, v: V): KR = mapper(k, v)
    }
    inner.toStream[KR](mapperJ)
  }

  def through(topic: String,
              storeName: String)
             (implicit keySerde: Serde[K], valSerde: Serde[V]): KTableS[K, V] = {
    inner.through(keySerde, valSerde, topic, storeName)
  }

  def through(partitioner: (K, V, Int) => Int,
              topic: String,
              storeName: String,
              keySerde: Serde[K],
              valSerde: Serde[V]): KTableS[K, V] = {
    val partitionerJ: StreamPartitioner[K, V] = new StreamPartitioner[K,V] {
      override def partition(key: K, value: V, numPartitions: Int): Integer = partitioner(key, value, numPartitions)
    }
    inner.through(keySerde, valSerde, partitionerJ, topic, storeName)
  }

  def to(topic: String, keySerde: Serde[K], valSerde: Serde[V]) = {
    inner.to(keySerde, valSerde, topic)
  }

  def to(partitioner: (K, V, Int) => Int,
         topic: String, keySerde: Serde[K], valSerde: Serde[V]) = {
    val partitionerJ: StreamPartitioner[K, V] = new StreamPartitioner[K, V]{
      override def partition(key: K, value: V, numPartitions: Int): Integer = partitioner(key, value, numPartitions)
    }
    inner.to(keySerde, valSerde, partitionerJ, topic)
  }

  def groupBy[KR, VR](selector: (K, V) => (KR, VR)): KGroupedTableS[KR, VR] = {
    val selectorJ: KeyValueMapper[K, V, KeyValue[KR, VR]] = new KeyValueMapper[K, V, KeyValue[KR, VR]]{
      override def apply(key: K, value: V): KeyValue[KR, VR] = {
        val res = selector(key, value)
        new KeyValue[KR, VR](res._1, res._2)
      }
    }
    inner.groupBy(selectorJ)
  }

  def groupBy[KR, VR](selector: (K, V) => (KR, VR),
                      keySerde: Serde[KR],
                      valueSerde: Serde[VR]): KGroupedTableS[KR, VR] = {
    val selectorJ: KeyValueMapper[K, V, KeyValue[KR, VR]] = new KeyValueMapper[K, V, KeyValue[KR, VR]]{
      override def apply(key: K, value: V): KeyValue[KR, VR] = {
        val res = selector(key, value)
        new KeyValue[KR, VR](res._1, res._2)
      }
    }
    inner.groupBy(selectorJ, keySerde, valueSerde)
  }

  def join[VO, VR](other: KTableS[K, VO],
                   joiner: (V, VO) => VR): KTableS[K, VR] = {
    val joinerJ: ValueJoiner[V, VO, VR] = new ValueJoiner[V, VO, VR]{
      override def apply(value1: V, value2: VO): VR = joiner(value1, value2)
    }
    inner.join[VO, VR](other.inner, joinerJ)
  }

  def leftJoin[VO, VR](other: KTableS[K, VO],
                       joiner: (V, VO) => VR): KTableS[K, VR] = {
    val joinerJ: ValueJoiner[V, VO, VR] = new ValueJoiner[V, VO, VR]{
      override def apply(v1: V, v2: VO): VR = joiner(v1, v2)
    }
    inner.leftJoin[VO, VR](other.inner, joinerJ)
  }

  def outerJoin[VO, VR](other: KTableS[K, VO],
                        joiner: (V, VO) => VR): KTableS[K, VR] = {
    val joinerJ: ValueJoiner[V, VO, VR] = new ValueJoiner[V, VO, VR]{
      override def apply(v1: V, v2: VO): VR = joiner(v1, v2)
    }
    inner.outerJoin[VO, VR](other.inner, joinerJ)
  }

  def getStoreName: String =
    inner.getStoreName

}