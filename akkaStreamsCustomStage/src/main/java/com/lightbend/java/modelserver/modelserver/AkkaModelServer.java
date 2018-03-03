package com.lightbend.java.modelserver.modelserver;

import akka.NotUsed;
import akka.actor.ActorSystem;
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
import akka.stream.javadsl.Source;
import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.java.model.DataConverter;
import com.lightbend.java.modelserver.queryablestate.QueriesAkkaHTTPResource;
import com.lightbend.model.Winerecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import scala.concurrent.ExecutionContextExecutor;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

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

        // Data Source
        Source<Winerecord.WineRecord, Consumer.Control> dataStream =
            Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(ApplicationKafkaParameters.DATA_TOPIC))
                .map(record -> DataConverter.convertData(record.value()))
                .filter(record -> record.isPresent()).map(record -> record.get());

        // Model Predictions
        Source<Optional<Double>,ReadableModelStore> modelPredictions = null; /*  // Remove null
        dataStream.viaMat(new ModelStage(), Keep.right()).map(result -> {  */
        // Exercise: Provide implementation here.
        // Add printing of the execution results. Ensure that you distinguish the "no model" case
        // 1. Match on the `result.processed` (a Boolean).
        // 2. If true, print fields in the `result` object and return the `result.result` in a `Some`.
        // 3. If false, print that no model is available and return `None`.
        // });   Uncomment this one
        //
        // Exercise: Provide implementation here.
        // 1. Write results to a new Kafka topic.
        //    See https://doc.akka.io/docs/akka-stream-kafka/current/producer.html#producer-as-a-sink

        ReadableModelStore modelStateStore =
                modelPredictions
                        .to(Sink.ignore())      // we do not read the results directly
                        // try changing Sink.ignore() to Sink.foreach(x -> System.out.println(x))) What gets printed?
                        .run(materializer);     // we run the stream, materializing the stage's StateStore

        // model stream
        // Exercise: Provide implementation here.
        // Implement model processing (as was done in previous examples). The steps here should be:
        // 1. Convert incoming byte array message to ModelToServe (ModelToServe.fromByteArray)
        // 2. Make sure you ignore records that failed to marshal
        // 3. Convert ModelToserve to a computable model (ModelWithDescriptor.fromModelToServe)
        // 4. Make sure you ignore models that failed to convert
        // 5. Update the stage with a new model (modelStateStore.setModel)

        startRest(system,materializer,modelStateStore);
    }

    // Serve model status: http://localhost:5500/state
    private static void startRest(ActorSystem system, ActorMaterializer materializer, ReadableModelStore reader) {
        QueriesAkkaHTTPResource resource = new QueriesAkkaHTTPResource(reader);
        Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = resource.createRoute().flow(system, materializer);
        Http http = Http.get(system);
        CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(host,port), materializer);
        System.out.println("Starting models observer on host " + host + " port " + port);
    }
}
