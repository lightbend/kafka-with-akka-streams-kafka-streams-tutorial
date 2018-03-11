package com.lightbend.scala.kafkastreams.modelserver.standardstore

import java.util.{HashMap, Properties}

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{KStream, Predicate, ValueMapper}
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters._
import com.lightbend.kafka.scala.streams.StreamsBuilderS
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.kafkastreams.modelserver._
import com.lightbend.scala.modelServer.model.{DataRecord, ModelToServe, ModelWithDescriptor, ServingResult}
import com.lightbend.scala.kafkastreams.store.store.ModelStateSerde

import scala.util.Try

object StandardStoreStreamBuilder {

  def createStreamsFluent(streamsConfiguration: Properties) : KafkaStreams = { // Create topology

    // Store definition
    val logConfig = new HashMap[String, String]
    val storeSupplier = Stores.inMemoryKeyValueStore(STORE_NAME)
    val storeBuilder = Stores.keyValueStoreBuilder(storeSupplier, Serdes.Integer, new ModelStateSerde).withLoggingEnabled(logConfig)

    // Create Stream builder
    val builder = new StreamsBuilderS
    // Data input streams
    val data  = builder.stream[Array[Byte], Array[Byte]](DATA_TOPIC)
    val models  = builder.stream[Array[Byte], Array[Byte]](MODELS_TOPIC)

    // DataStore
    builder.addStateStore(storeBuilder)


    // Data Processor

    data
      .mapValues(value => DataRecord.fromByteArray(value))
      .filter((key, value) => (value.isSuccess))
      .transform(() => new DataProcessor, STORE_NAME)
      .mapValues(value => {
        if(value.processed) println(s"Calculated quality - ${value.result} calculated in ${value.duration} ms")
        else println("No model available - skipping")
        value
      })

    //Models Processor
    models
      .mapValues(value => ModelToServe.fromByteArray(value))
      .filter((key, value) => (value.isSuccess))
      .mapValues(value => ModelWithDescriptor.fromModelToServe(value.get))
      .filter((key, value) => (value.isSuccess))
      .process(() => new ModelProcessor, STORE_NAME)

    // Create and build topology
    val topology = builder.build
    println(topology.describe)

    return new KafkaStreams(topology, streamsConfiguration)

  }

  def createStreams(streamsConfiguration: Properties) : KafkaStreams = { // Create topology

    // Store definition
    val logConfig = new HashMap[String, String]
    val storeSupplier = Stores.inMemoryKeyValueStore(STORE_NAME)
    val storeBuilder = Stores.keyValueStoreBuilder(storeSupplier, Serdes.Integer, new ModelStateSerde).withLoggingEnabled(logConfig)

    // Create Stream builder
    val builder = new StreamsBuilder
    // Data input streams
    val data : KStream[Array[Byte], Array[Byte]] = builder.stream(DATA_TOPIC)
    val models : KStream[Array[Byte], Array[Byte]] = builder.stream(MODELS_TOPIC)

    // DataStore
    builder.addStateStore(storeBuilder)

    // Data Processor
    data
      .mapValues[Try[WineRecord]](new DataValueMapper().asInstanceOf[ValueMapper[Array[Byte], Try[WineRecord]]])
      .filter(new DataValueFilter().asInstanceOf[Predicate[Array[Byte], Try[WineRecord]]])
      .transform(() => new DataProcessorKV, STORE_NAME)
      .mapValues[ServingResult](new ResultPrinter())
    // Models Processor
    models
      .mapValues[Try[ModelToServe]](new ModelValueMapper().asInstanceOf[ValueMapper[Array[Byte],Try[ModelToServe]]])
      .filter(new ModelValueFilter().asInstanceOf[Predicate[Array[Byte], Try[ModelToServe]]])
      .mapValues[Try[ModelWithDescriptor]](new ModelDescriptorMapper().asInstanceOf[ValueMapper[Try[ModelToServe],Try[ModelWithDescriptor]]])
      .filter((new ModelDescriptorFilter().asInstanceOf[Predicate[Array[Byte], Try[ModelWithDescriptor]]]))
      .process(() => new ModelProcessor, STORE_NAME)

    // Create and build topology
    val topology = builder.build
    println(topology.describe)

    return new KafkaStreams(topology, streamsConfiguration)

  }
}
