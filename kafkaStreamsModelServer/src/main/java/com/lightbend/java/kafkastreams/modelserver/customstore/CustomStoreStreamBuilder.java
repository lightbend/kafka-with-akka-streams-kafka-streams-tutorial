package com.lightbend.java.kafkastreams.modelserver.customstore;

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.java.kafkastreams.store.store.custom.ModelStateStoreBuilder;
import com.lightbend.java.model.DataConverter;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Use the Kafka Streams DSL to define the application streams.
 * Use a custom storage implementation for the running state.
 */
public class CustomStoreStreamBuilder {

    public static KafkaStreams createStreams(final Properties streamsConfiguration) {

        // Store definition
        Map<String, String> logConfig = new HashMap<>();
        ModelStateStoreBuilder storeBuilder = new ModelStateStoreBuilder(ApplicationKafkaParameters.STORE_NAME).withLoggingEnabled(logConfig);

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
        // Exercise:
        // We just printed the result, but we didn't do anything else with it.
        // In particular, we might want to write the results to a new Kafka topic.
        // 1. Modify the "client" to create a new output topic.
        // 2. Modify KafkaModelServer to add the configuration for the new topic.
        // 3. Add a final step that writes the results to the new topic.
        //    Consult the Kafka Streams documentation for details.

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
