package com.lightbend.java.custom.modelserver;

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.java.custom.modelserver.store.ModelStateStoreBuilder;
import com.lightbend.java.custom.queriablestate.QueriesRestService;
import com.lightbend.java.model.DataConverter;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;

import java.util.HashMap;
import java.util.Map;
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
                .transform(DataProcessor::new,ApplicationKafkaParameters.STORE_NAME)
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

    // Surf to http://localhost:8888/state/instances for the list of currently deployed instances.
    // Then surf to http://localhost:8888/state/value for the current state of execution for a given model.
    static QueriesRestService startRestProxy(final KafkaStreams streams, final int port) throws Exception {
        final QueriesRestService restService = new QueriesRestService(streams);
        restService.start(port);
        return restService;
    }
}
