package com.lightbend.java.modelserver.modelserver;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.ActorMaterializer;
import akka.stream.SourceShape;
import akka.stream.javadsl.GraphDSL;
import akka.stream.javadsl.Source;
import com.lightbend.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.model.DataConverter;
import com.lightbend.model.ModelWithDescriptor;
import com.lightbend.model.Winerecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import scala.concurrent.ExecutionContextExecutor;

import java.util.Optional;


public class AkkaModelServer {

    private static final String host = "localhost";
    private static final int port = 5500;

    public static void main(String [ ] args) throws Throwable {

        // Akka
        final ActorSystem system = ActorSystem.create("ModelServing");
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final ExecutionContextExecutor executionContext = system.dispatcher();

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

        // Model Source
        Source<ModelWithDescriptor, Consumer.Control> modelStream =
            Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(ApplicationKafkaParameters.MODELS_TOPIC))
                .map(record -> DataConverter.convertModel(record.value()))
                .filter(record -> record.isPresent()).map(record -> record.get())
                .map(record -> DataConverter.convertModel(record))
                .filter(record -> record.isPresent()).map(record -> record.get());

        // Data Source
        Source<Winerecord.WineRecord, Consumer.Control> dataStream =
            Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(ApplicationKafkaParameters.DATA_TOPIC))
                .map(record -> DataConverter.convertData(record.value()))
                .filter(record -> record.isPresent()).map(record ->record.get());

        ModelStage modelProcessor = new ModelStage();
/*
        Source<Optional<Double>, ReadableModelStore> predictions = Source.fromGraph(
                GraphDSL.create(
                        builder -> {
                            // wire together the input streams with the model stage (2 in, 1 out)
        /*
                            dataStream --> |       |
                                           | model | -> predictions
                            modelStream -> |       |
        */
/*
                            builder.from(builder.add(modelStream)).toInlet(modelProcessor.modelRecordIn);
                            builder.from(builder.add(dataStream)).toInlet(modelProcessor.dataRecordIn);
                            return SourceShape.of(modelProcessor.scoringResultOut);
                        }
                )
        );
*/
    }

/*
    private static void startRest(ActorSystem system, ActorMaterializer materializer, ActorRef router) {
        QueriesAkkaHTTPResource resource = new QueriesAkkaHTTPResource(router);
        Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = resource.createRoute().flow(system, materializer);
        Http http = Http.get(system);
        CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(host,port), materializer);
        System.out.println("Starting models observer on host " + host + " port " + port);
    } */
}
