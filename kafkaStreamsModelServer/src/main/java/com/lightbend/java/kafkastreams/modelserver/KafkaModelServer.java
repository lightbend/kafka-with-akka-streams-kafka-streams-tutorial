package com.lightbend.java.kafkastreams.modelserver;

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.java.kafkastreams.modelserver.customstore.CustomStoreStreamBuilder;
import com.lightbend.java.kafkastreams.modelserver.memorystore.MemoryStoreStreamBuilder;
import com.lightbend.java.kafkastreams.modelserver.standardstore.StandardStoreStreamBuilder;
import com.lightbend.java.kafkastreams.queriablestate.StoppableService;
import com.lightbend.java.kafkastreams.queriablestate.inmemory.RestServiceInMemory;
import com.lightbend.java.kafkastreams.queriablestate.withstore.RestServiceWithStore;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Arrays;
import java.util.Properties;


/**
 * Created by boris on 6/28/17.
 */

public class KafkaModelServer {

    final static int port=8888;                             // Port for queryable state

    private static void help(String message, int exitCode) {
        System.out.printf("%s\n", message);
        System.out.printf("AkkaModelServer -h | --help");
        System.out.printf("By default, uses a custom state store implementation.\n");
        System.out.printf("Modify the code to use other implementations (see comments)\n");
        System.exit(exitCode);
    }

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

        if (args.length > 0 && (args[0] == "-h" || args[0] == "--help")) {
            help("", 0);
        }

        // Uses the custom store by default. To use one of the others,
        // just switch which PAIR of lines are commented out.
        final KafkaStreams streams = CustomStoreStreamBuilder.createStreams(streamsConfiguration);
        final StoppableService restService = RestServiceWithStore.startRestProxy(streams, port, "custom");
        // Use the in-memory store
//        final KafkaStreams streams = MemoryStoreStreamBuilder.createStreams(streamsConfiguration);
//        final StoppableService restService = RestServiceInMemory.startRestProxy(streams, port);

        // Use the standard store as the default.
//        final KafkaStreams streams = StandardStoreStreamBuilder.createStreams(streamsConfiguration);
//        final StoppableService restService = RestServiceWithStore.startRestProxy(streams, port, "standard");

        // Set Stream exception handler
        streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                System.out.println("Uncaught exception on thread " + t + " " + e.toString());
            }
        });

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                streams.close();
                restService.stop();
            } catch (Exception e) {
                // ignored
            }
        }));
        // Start streams
        streams.start();
    }
}
