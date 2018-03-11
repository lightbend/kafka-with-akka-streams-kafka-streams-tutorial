package com.lightbend.java.akkastreams.modelServer;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;
import akka.util.Timeout;
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.java.model.DataConverter;
import com.lightbend.java.model.ModelWithDescriptor;
import com.lightbend.model.Winerecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import scala.concurrent.ExecutionContextExecutor;

import java.util.concurrent.TimeUnit;

public class AkkaModelServer {

    private static final String host = "localhost";
    private static final int port = 5500;

    public static void main(String [ ] args) throws Throwable {

        // Akka
        final ActorSystem system = ActorSystem.create("ModelServing");
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final ExecutionContextExecutor executionContext = system.dispatcher();

        final Timeout askTimeout = Timeout.apply(5, TimeUnit.SECONDS);

        // Kafka config
        final ConsumerSettings<byte[], byte[]> dataConsumerSettings =
                ConsumerSettings.create(system, new ByteArrayDeserializer(), new ByteArrayDeserializer())
                        .withBootstrapServers(ApplicationKafkaParameters.KAFKA_BROKER)
                        .withGroupId(ApplicationKafkaParameters.DATA_GROUP)
                        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        final ConsumerSettings<byte[], byte[]> modelConsumerSettings =
                ConsumerSettings.create(system, new ByteArrayDeserializer(), new ByteArrayDeserializer())
                        .withBootstrapServers(ApplicationKafkaParameters.KAFKA_BROKER)
                        .withGroupId(ApplicationKafkaParameters.MODELS_GROUP)
                        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


        // Data stream
        Source<Winerecord.WineRecord, Consumer.Control> dataStream =
                Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(ApplicationKafkaParameters.DATA_TOPIC))
                        .map(record -> DataConverter.convertData(record.value()))
                        .filter(record -> record.isPresent()).map(record ->record.get());

        // Model stream
        Source<ModelWithDescriptor, Consumer.Control> modelStream =
        Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(ApplicationKafkaParameters.MODELS_TOPIC))
                .map(record -> DataConverter.convertModel(record.value()))
                .filter(record -> record.isPresent()).map(record -> record.get())
                .map(record -> DataConverter.convertModel(record))
                .filter(record -> record.isPresent()).map(record -> record.get());

        // Use custom stage
        ModelServerProcessor.stageModelServerProcessor(dataStream, modelStream, system, materializer);

        // Use actor model
//        ModelServerProcessor.actorModelServerProcessor(dataStream, modelStream, system, materializer);
    }
}
