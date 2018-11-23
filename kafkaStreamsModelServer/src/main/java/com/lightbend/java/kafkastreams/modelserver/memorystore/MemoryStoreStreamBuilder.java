package com.lightbend.java.kafkastreams.modelserver.memorystore;

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;

import java.util.Properties;

/**
 * Use the lower level topology API for defining the Kafka Streams processing "nodes".
 * Store the running state in memory only, which of course is not durable.
 */
public class MemoryStoreStreamBuilder {

    public static KafkaStreams createStreams(final Properties streamsConfiguration) {

        // Exercise:
        // We use a `PrintProcessor` to print the result, but not do anything else with it.
        // In particular, we might want to write the results to a new Kafka topic.
        // 1. Modify the "client" to create a new output topic.
        // 2. Modify KafkaModelServer to add the configuration for the new topic.
        // 3. Add a final step that writes the results to the new topic.
        //    Consult the Kafka Streams Topology documentation for details.

        // Create topology
        Topology topology = new Topology();

        // Data input streams
        topology.addSource("data", ApplicationKafkaParameters.DATA_TOPIC);
        topology.addSource("models", ApplicationKafkaParameters.MODELS_TOPIC);

        // Processors
        topology.addProcessor("result", () -> new DataProcessor(), "data");
        topology.addProcessor("printer", () -> new PrintProcessor(), "result");
        topology.addProcessor("model processor", () -> new ModelProcessor(), "models");

        // print topology
        System.out.println(topology.describe());

        return new KafkaStreams(topology, streamsConfiguration);
    }
}
