package com.lightbend.modelserver.withstore;

import com.lightbend.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.modelserver.store.ModelStateSerde;
import com.lightbend.modelserver.store.ModelStateStoreSupplier;
import com.lightbend.modelserver.store.StoreState;
import com.lightbend.queriablestate.QueriesRestService;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStreamBuilder;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Created by boris on 6/28/17.
 */
@SuppressWarnings("Duplicates")
public class ModelServerWithStore {

    final static int port=8888;                             // Port for queryable state

    public static void main(String [ ] args) throws Throwable {

        Properties streamsConfiguration = new Properties();
        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "interactive-queries-example");
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "interactive-queries-example-client");
        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, ApplicationKafkaParameters.LOCAL_KAFKA_BROKER);
        // Provide the details of our embedded http service that we'll use to connect to this streams
        // instance and discover locations of stores.
        streamsConfiguration.put(StreamsConfig.APPLICATION_SERVER_CONFIG, "localhost:" + port);
        final File example = Files.createTempDirectory(new File("/tmp").toPath(), "example").toFile();
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, example.getPath());
        // Create topology
        final KafkaStreams streams = createStreams(streamsConfiguration);
        streams.cleanUp();
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

        Serde<StoreState> stateSerde = new ModelStateSerde();
        ByteArrayDeserializer deserializer = new ByteArrayDeserializer();
        ModelStateStoreSupplier storeSupplier = new ModelStateStoreSupplier("modelStore", stateSerde);


        KStreamBuilder builder = new KStreamBuilder();
        // Data input streams

        builder.addSource("data-source", deserializer, deserializer, ApplicationKafkaParameters.DATA_TOPIC)
                .addProcessor("ProcessData", DataProcessorWithStore::new, "data-source");
        builder.addSource("model-source", deserializer, deserializer, ApplicationKafkaParameters.MODELS_TOPIC)
                .addProcessor("ProcessModels", ModelProcessorWithStore::new, "model-source");
        builder.addStateStore(storeSupplier, "ProcessData", "ProcessModels");


        return new KafkaStreams(builder, streamsConfiguration);
    }

    static QueriesRestService startRestProxy(final KafkaStreams streams, final int port) throws Exception {
        final QueriesRestService restService = new QueriesRestService(streams);
        restService.start(port);
        return restService;
    }
}
