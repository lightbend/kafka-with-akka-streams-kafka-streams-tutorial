package com.lightbend.java.modelserver.actor.modelServer;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.util.Timeout;
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.java.model.DataConverter;
import com.lightbend.java.model.ServingResult;
import com.lightbend.java.modelserver.actor.actors.ModelServingManager;
import com.lightbend.java.modelserver.actor.queryablestate.QueriesAkkaHTTPResource;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import scala.concurrent.ExecutionContextExecutor;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.pattern.PatternsCS.ask;

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

        // Router
        final ActorRef router = system.actorOf(Props.create(ModelServingManager.class));

        // Model stream processing
        Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(ApplicationKafkaParameters.MODELS_TOPIC))
                .map(record -> DataConverter.convertModel(record.value()))
                .filter(record -> record.isPresent()).map(record -> record.get())
                .map(record -> DataConverter.convertModel(record))
                .filter(record -> record.isPresent()).map(record -> record.get())
                .mapAsync(1, record -> ask(router, record, askTimeout))
                .runWith(Sink.ignore(), materializer);

        // Data stream processing
        Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(ApplicationKafkaParameters.DATA_TOPIC))
                .map(record -> DataConverter.convertData(record.value()))
                .filter(record -> record.isPresent()).map(record ->record.get())
                .mapAsync(1, record -> ask(router, record, askTimeout))
                .map(record -> (ServingResult)record)
                .runWith(Sink.foreach(record -> {
                    if(record.isProcessed()) System.out.println("Calculated quality - " + record.getResult() + " in " + record.getDuration() + "ms");
                    else System.out.println("No model available - skipping");
                }), materializer);
        startRest(system,materializer,router);
    }

    // See http://localhost:5500/models
    // Then select a model shown and try http://localhost:5500/state/<model>, e.g., http://localhost:5500/state/wine
    private static void startRest(ActorSystem system, ActorMaterializer materializer, ActorRef router) {
        QueriesAkkaHTTPResource resource = new QueriesAkkaHTTPResource(router);
        Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = resource.createRoute().flow(system, materializer);
        Http http = Http.get(system);
        CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(host,port), materializer);
        System.out.println("Starting models observer on host " + host + " port " + port);
    }
}
