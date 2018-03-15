package com.lightbend.java.kafkastreams.modelserver.standardstore;

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.java.model.DataConverter;
import com.lightbend.java.kafkastreams.store.StoreState;
import com.lightbend.java.kafkastreams.store.store.ModelStateSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class StandardStoreStreamBuilder {

    public static KafkaStreams createStreams(final Properties streamsConfiguration) {

        // Store definition
        Map<String, String> logConfig = new HashMap<>();
        KeyValueBytesStoreSupplier storeSupplier = Stores.inMemoryKeyValueStore(ApplicationKafkaParameters.STORE_NAME);
        StoreBuilder<KeyValueStore<Integer, StoreState>> storeBuilder =
                Stores.keyValueStoreBuilder(storeSupplier, Serdes.Integer(),new ModelStateSerde())
                        .withLoggingEnabled(logConfig);

        // Create Stream builder
        StreamsBuilder builder = new StreamsBuilder();

        // Data input streams
        KStream<byte[], byte[]> data = builder.stream(ApplicationKafkaParameters.DATA_TOPIC);
        KStream<byte[], byte[]> models = builder.stream(ApplicationKafkaParameters.MODELS_TOPIC);

        // DataStore
        builder.addStateStore(storeBuilder);
        // Data Processor

        data
                .mapValues(value -> DataConverter.convertData(value))
                .filter((key, value) -> value.isPresent())
                .transform(DataProcessor::new, ApplicationKafkaParameters.STORE_NAME)
                .mapValues(value -> {
                    if(value.isProcessed()) System.out.println("Calculated quality - " + value.getResult() + " in " + value.getDuration() + "ms");
                    else System.out.println("No model available - skipping");
                    return value;
                });
        // Model Processor
        models
                .mapValues(value -> DataConverter.convertModel(value))
                .filter((key, value) -> value.isPresent())
                .mapValues(value -> DataConverter.convertModel(value))
                .filter((key, value) -> value.isPresent())
                .process(ModelProcessor::new,ApplicationKafkaParameters.STORE_NAME);

        // Create and build topology
        Topology topology = builder.build();
        System.out.println(topology.describe());

        return new KafkaStreams(topology, streamsConfiguration);
    }
}
