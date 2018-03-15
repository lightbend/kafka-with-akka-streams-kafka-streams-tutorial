package com.lightbend.java.kafkastreams.modelserver.memorystore;

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;

import java.util.Properties;

public class MemoryStoreStreamBuilder {

    public static KafkaStreams createStreams(final Properties streamsConfiguration) {


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
