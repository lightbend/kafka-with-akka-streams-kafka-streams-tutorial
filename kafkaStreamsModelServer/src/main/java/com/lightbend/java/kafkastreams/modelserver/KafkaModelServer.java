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

import java.util.Properties;


/**
 * Created by boris on 6/28/17.
 */

public class KafkaModelServer {

    final static int port=8888;                             // Port for queryable state

    private static void help(String message, int exitCode) {
        System.out.printf("%s\n", message);
        System.out.printf("AkkaModelServer -h | --help  m | memory | c | custom | s | standard\n\n");
        System.out.printf("-h | --help   Print this help and exit\n");
        System.out.printf(" m | memory   Use the in-memory (\"naive\") state store implementation\n");
        System.out.printf(" c | custom   Use the custom state store implementation (default)\n");
        System.out.printf(" s | standard Use the built-in (\"standard\") state store implementation\n\n");
        System.exit(exitCode);
    }

    private static class Streams_Rest {
        // Let's not obsess about hiding fields behind getters...
        public KafkaStreams streams;
        public StoppableService restService;

        public Streams_Rest(String whichStorage, Properties streamsConfiguration) throws Exception {
            if (whichStorage == "c" || whichStorage == "custom") {
                streams = CustomStoreStreamBuilder.createStreams(streamsConfiguration);
                restService = RestServiceWithStore.startRestProxy(streams, port, "custom");
            } else if (whichStorage == "m" || whichStorage == "memory") {
                streams = MemoryStoreStreamBuilder.createStreams(streamsConfiguration);
                restService = RestServiceInMemory.startRestProxy(streams, port);
            } else if (whichStorage == "s" || whichStorage == "standard") {
                streams = StandardStoreStreamBuilder.createStreams(streamsConfiguration);
                restService = RestServiceWithStore.startRestProxy(streams, port, "standard");
            } else {
                help("Unexpected storage type:" + whichStorage, 1);
            }
        }
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

        // You can either pick which one to run using a command-line argument, or for ease
        // of use with the IDE Run menu command, just switch which line is commented out.
//        String whichStorage = "custom";
//        String whichStorage = "memory";
        String whichStorage = "standard";
        if (args.length > 0 ) {
            whichStorage = args[0];
        }

        final Streams_Rest sr =
                new Streams_Rest(whichStorage, streamsConfiguration);

        // Set Stream exception handler
        sr.streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                System.out.println("Uncaught exception on thread " + t + " " + e.toString());
            }
        });

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                sr.streams.close();
                sr.restService.stop();
            } catch (Exception e) {
                // ignored
            }
        }));
        // Start streams
        sr.streams.start();
    }
}
