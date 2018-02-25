package com.lightbend.java.naive.modelserver;

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.java.naive.modelserver.queriablestate.QueriesRestService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import java.util.Properties;

/**
 * Created by boris on 6/28/17.
 */

public class ModelServer {

    final static int port=8888;                             // Port for queryable state

    public static void main(String [ ] args) throws Throwable {

        System.out.println("Using kafka brokers at " + ApplicationKafkaParameters.KAFKA_BROKER);

        Properties streamsConfiguration = new Properties();
        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-model-server");
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, ApplicationKafkaParameters.DATA_GROUP);
        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, ApplicationKafkaParameters.KAFKA_BROKER);
        // Provide the details of our embedded http service that we'll use to connect to this streams
        // instance and discover locations of stores.
        streamsConfiguration.put(StreamsConfig.APPLICATION_SERVER_CONFIG, "127.0.0.1:" + port);
        // Default serdes
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass());

        // Add a topic config by prefixing with topic
//        streamsConfiguration.put(StreamsConfig.topicPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG), "earliest");

        // Create topology
        final KafkaStreams streams = createStreams(streamsConfiguration);
        // Set Stream exception handler
        streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                System.out.println("Uncaught exception on thread " + t + " " + e.toString());
            }
        });

        // Start streams
        streams.start();
        // Start the Restful proxy for servicing remote access to state stores
        final QueriesRestService restService = startRestProxy(streams, port);
        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                streams.close();
                restService.stop();
            } catch (Exception e) {
                // ignored
            }
        }));
    }

    static KafkaStreams createStreams(final Properties streamsConfiguration) {


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

    // surf to http://localhost:8888/state/value
    static QueriesRestService startRestProxy(final KafkaStreams streams, final int port) throws Exception {
        final QueriesRestService restService = new QueriesRestService(streams);
        restService.start(port);
        return restService;
    }
}
