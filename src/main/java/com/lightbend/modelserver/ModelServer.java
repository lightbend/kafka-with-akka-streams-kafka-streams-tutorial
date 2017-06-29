package com.lightbend.modelserver;

import com.lightbend.kafka.ApplicationKafkaParameters;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

/**
 * Created by boris on 6/28/17.
 */
public class ModelServer {

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
        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                streams.close();
            } catch (Exception e) {
                // ignored
            }
        }));
    }

    static KafkaStreams createStreams(final Properties streamsConfiguration) {

        final Serde<byte[]> serde = Serdes.ByteArray();
        KStreamBuilder builder = new KStreamBuilder();
        // Data input stream
        KStream<byte[], byte[]> dataStream = builder.stream(serde, serde, ApplicationKafkaParameters.DATA_TOPIC);
        KStream<byte[], byte[]> modelStream = builder.stream(serde, serde, ApplicationKafkaParameters.MODELS_TOPIC);
        // Model processing
        modelStream.mapValues(value -> DataConverter.convertModel(value)).
                flatMapValues(value -> Arrays.asList(value.get())).
                foreach(new ForeachAction<byte[], ModelDescriptor>() {
                    public void apply(byte[] key, ModelDescriptor value) {
                        ModelState.getInstance().updateModel(value);
                    }
                });
        // Data processing
        dataStream.mapValues(value -> DataConverter.convertData(value)).
                flatMapValues(value -> Arrays.asList(value.get())).
                mapValues(value -> ModelState.getInstance().serve(value)).
                foreach(new ForeachAction<byte[], Optional<Double>>() {
                    public void apply(byte[] key, Optional<Double> value) {
                        System.out.println("Result" + value);
                    }
                });
        return new KafkaStreams(builder, streamsConfiguration);
    }
}
