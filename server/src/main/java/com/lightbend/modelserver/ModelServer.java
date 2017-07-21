package com.lightbend.modelserver;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.scaladsl.settings.ServerSettings;
import akka.japi.Pair;
import akka.kafka.AutoSubscription;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.SourceShape;
import akka.stream.javadsl.GraphDSL;
import akka.stream.javadsl.Source;
import com.lightbend.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.model.CurrentModelDescriptor;
import com.lightbend.model.DataConverter;
import com.lightbend.model.Winerecord;
import com.lightbend.modelserver.store.ReadableModelStateStore;
import com.lightbend.queriablestate.QueriesAkkaHttpService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.util.Collections;
import java.util.OptionalDouble;

public class ModelServer {

  // Port for queryable state
  final static int port = 8888;

  private static ActorSystem system;
  private static Materializer mat;
  private static ConsumerSettings<byte[], byte[]> consumerSettings;

  public static void main(String[] args) throws Throwable {
    system = ActorSystem.create("ModelServer");
    mat = ActorMaterializer.create(system);

      consumerSettings = ConsumerSettings.create(system, new ByteArrayDeserializer(), new ByteArrayDeserializer())
        .withBootstrapServers(ApplicationKafkaParameters.LOCAL_KAFKA_BROKER)
        .withGroupId("model-server-group") // TODO is this ok?
        .withClientId("interactive-queries-example-client");

    final Source<Pair<Winerecord.WineRecord, OptionalDouble>, ReadableModelStateStore> predictionsSource = wireStreams(consumerSettings);

    startRest(predictionsSource, port);
  }

  static Source<Pair<Winerecord.WineRecord, OptionalDouble>, ReadableModelStateStore> wireStreams(final ConsumerSettings<byte[], byte[]> streamsConfiguration) {
    final AutoSubscription modelsTopic = Subscriptions.topics(ApplicationKafkaParameters.MODELS_TOPIC);
    final Source<ConsumerRecord<byte[], byte[]>, akka.kafka.javadsl.Consumer.Control> models =
      akka.kafka.javadsl.Consumer.<byte[], byte[]>atMostOnceSource(consumerSettings, modelsTopic);

    final AutoSubscription dataTopic = Subscriptions.topics(ApplicationKafkaParameters.DATA_TOPIC);
    final Source<ConsumerRecord<byte[], byte[]>, akka.kafka.javadsl.Consumer.Control> datas =
      akka.kafka.javadsl.Consumer.<byte[], byte[]>atMostOnceSource(consumerSettings, dataTopic);


    // Model stream
    final Source<Winerecord.WineRecord, Consumer.Control> wineRecords = models
      .map(value -> DataConverter.convertData(value.value()))
      .mapConcat(value -> Collections.singletonList(value.get()));

    // Data stream
    final Source<CurrentModelDescriptor, Consumer.Control> modelDescriptors = datas
      .map(value -> DataConverter.convertModel(value.value()))
      .mapConcat(value -> Collections.singletonList(value.get()));


    final Source<Pair<Winerecord.WineRecord, OptionalDouble>, ReadableModelStateStore> modelPredictions  = 
      Source.fromGraph(GraphDSL.create3(
        wineRecords, modelDescriptors, new ModelStage(),
        (wineStreamControl, modelChangeStreamControl, notUsed) -> notUsed, // materialized values, no need for control values of kafka consumers
        (b, wineData, modelChange, model) -> { // wire together the input streams with the model stage (2 in, 1 out)
            
          /* 
                      wine --> |       |
                               | model | -> (Source of predictions)
              model changes -> |       | 
           */

        b
          .from(wineData).toInlet(model.wineRecordIn)
          .from(modelChange).toInlet(model.modelIn);

        return new SourceShape<>(model.qualityOut);
      }));

    return modelPredictions.map(value -> {
      System.out.println("Result" + value);
      return value;
    });
  }

  static void startRest(final Source<Pair<Winerecord.WineRecord, OptionalDouble>, ReadableModelStateStore> predictions, final int port) throws Exception {
    final QueriesAkkaHttpService httpService = new QueriesAkkaHttpService(predictions);
    httpService.startServer("localhost", port); // TODO translated it, but does not seem to make much sense if kafka streams not used?
  }
}
