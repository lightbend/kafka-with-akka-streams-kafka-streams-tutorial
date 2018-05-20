package com.lightbend.java.akkastreams.modelserver;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kafka.javadsl.Consumer;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.Timeout;
import com.lightbend.java.akkastreams.modelserver.actors.ModelServingManager;
import com.lightbend.java.akkastreams.modelserver.stage.ModelStage;
import com.lightbend.java.akkastreams.modelserver.stage.ReadableModelStore;
import com.lightbend.java.akkastreams.queryablestate.actors.RestServiceActors;
import com.lightbend.java.akkastreams.queryablestate.inmemory.RestServiceInMemory;
import com.lightbend.java.model.ModelWithDescriptor;
import com.lightbend.java.model.ServingResult;
import com.lightbend.model.Winerecord;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ModelServerProcessor {

    private static final Timeout askTimeout = Timeout.apply(5, TimeUnit.SECONDS);

    public interface ModelServerProcessorStreamCreator {
        void createStreams(Source<Winerecord.WineRecord, Consumer.Control> dataStream,
                           Source<ModelWithDescriptor, Consumer.Control> modelStream,
                           ActorSystem system, ActorMaterializer materializer);
    }

    public static class ActorModelServerProcessor implements ModelServerProcessorStreamCreator {

        public void createStreams(Source<Winerecord.WineRecord, Consumer.Control> dataStream,
                                  Source<ModelWithDescriptor, Consumer.Control> modelStream,
                                  ActorSystem system, ActorMaterializer materializer) {

            System.out.println("*** Using the Actor-based model server implementation ***");

            // Router
            final ActorRef router = system.actorOf(Props.create(ModelServingManager.class));

            // Data Stream processing
            dataStream
                    .ask(1, router, ServingResult.class, askTimeout)
//                    .mapAsync(1, record -> ask(router, record, askTimeout))
//                    .map(record -> (ServingResult) record)
                    .runWith(Sink.foreach(record -> {
                        if (record.isProcessed())
                            System.out.println("Calculated quality - " + record.getResult() + " in " + record.getDuration() + "ms");
                        else System.out.println("No model available - skipping");
                    }), materializer);
            // Exercise:
            // We just used `Sink.foreach`, which iterates through the records, prints output, but doesn't
            // return a value. (In functional programming terms, it's "pure side effects")
            // In particular, we might want to write the results to a new Kafka topic.
            // 1. Modify the "client" to create a new output topic.
            // 2. Modify AkkaModelServer to add the configuration for the new topic. For example, copy and adapt
            //    `dataConsumerSettings` for a new producer instead of a consumer.
            // 3. Replace `Sink.foreach` with logic to write the results to the new Kafka topic.
            //    Also keep the current `println` output for convenience. For information on writing to Kafka
            //    from Akka Streams, see:
            //    https://doc.akka.io/docs/akka-stream-kafka/current/producer.html#producer-as-a-sink

            // Exercise:
            // Repeat the previous exercise, but write the results to the local file system instead (easier).

            // Model Stream processing
            modelStream
                    .ask(1, router, String.class, askTimeout)
//                    .mapAsync(1, record -> ask(router, record, askTimeout))
                    .runWith(Sink.ignore(), materializer);

            // Rest service
            RestServiceActors.startRest(system, materializer, router);
        }
    }

    public static class CustomStageModelServerProcessor implements ModelServerProcessorStreamCreator {

        public void createStreams(Source<Winerecord.WineRecord, Consumer.Control> dataStream,
                                  Source<ModelWithDescriptor, Consumer.Control> modelStream,
                                  ActorSystem system, ActorMaterializer materializer) {

            System.out.println("*** Using the Custom Stage model server implementation ***");

            // Data Stream Processing
            Source<Optional<Double>, ReadableModelStore> modelPredictions =
                    dataStream.viaMat(new ModelStage(), Keep.right()).map(result -> {
                        if (result.isProcessed()) {
                            System.out.println("Calculated quality - " + result.getResult() + " in " + result.getDuration() + "ms");
                            return Optional.of(result.getResult());
                        } else {
                            System.out.println("No model available - skipping");
                            return Optional.empty();
                        }
                    });

            ReadableModelStore modelStateStore =
                    modelPredictions
                            .to(Sink.ignore())      // we do not read the results directly
                            .run(materializer);     // we run the stream, materializing the stage's StateStore

            // model stream processing
            modelStream.runForeach(model -> modelStateStore.setModel(model), materializer);

            // Rest service
            RestServiceInMemory.startRest(system, materializer, modelStateStore);
        }
    }
}