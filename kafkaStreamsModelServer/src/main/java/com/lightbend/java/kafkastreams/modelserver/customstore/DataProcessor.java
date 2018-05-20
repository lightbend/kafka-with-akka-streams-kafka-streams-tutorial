package com.lightbend.java.kafkastreams.modelserver.customstore;

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.java.kafkastreams.store.store.custom.ModelStateStore;
import com.lightbend.java.model.ModelServingInfo;
import com.lightbend.java.model.ServingResult;
import com.lightbend.model.Winerecord;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by boris on 7/12/17.
 * used
 * https://github.com/bbejeck/kafka-streams/blob/master/src/main/java/bbejeck/processor/stocks/StockSummaryProcessor.java
 */
public class DataProcessor implements Transformer<byte[], Optional<Winerecord.WineRecord>, KeyValue<byte[], ServingResult>> {

    private ModelStateStore modelStore;

    // Exercise:
    // Currently, one model for each kind of data is returned. For model serving,
    // we discussed in the presentation that you might want a set of workers for better
    // scalability through parallelism. (We discussed it in the context of the Akka Streams
    // example.) The line below,
    //   val quality = model.score(dataRecord.get).asInstanceOf[Double]
    // is where scoring is invoked. Modify this class to create a set of one or more
    // workers. Choose which one to use for a record randomly, round-robin, or whatever.
    // Add this feature without changing the public API of the class, so it's transparent
    // to users.
    // However, simply having a collection of servers won't help performance, because the current
    // invocation is synchronous. So, try adapting the Akka Actors example of model serving, with
    // a manager/router actor, so that you can invoke scoring asynchronously. How would you
    // properly integrate this approach with tbe Kafka Streams logic below?

    // Exercise:
    // One technique used to improve scoring performance is to score each record with a set
    // of models and then pick the best result. "Best result" could mean a few things:
    // 1. The score includes a confidence level and the result with the highest confidence wins.
    // 2. To met latency requirements, at least one of the models is faster than the latency window,
    //    but less accurate. Once the latency window expires, the fast result is returned if the
    //    slower models haven't returned a result in time.
    // Modify the model management and scoring logic to implement one or both scenarios. Again,
    // using Akka Actors or another concurrency library will be required.

    @Override
    public KeyValue<byte[], ServingResult> transform(byte[] key, Optional<Winerecord.WineRecord> dataRecord) {
        if(modelStore.getNewModel() != null){
            // update the model
            if(modelStore.getCurrentModel() != null)
                modelStore.getCurrentModel().cleanup();
            modelStore.setCurrentModel(modelStore.getNewModel());
            modelStore.setCurrentServingInfo(new ModelServingInfo(modelStore.getNewServingInfo().getName(),
                    modelStore.getNewServingInfo().getDescription(), System.currentTimeMillis()));
            modelStore.setNewServingInfo(null);
            modelStore.setNewModel(null);
        }
        // Actually score
        ServingResult result;
        if(modelStore.getCurrentModel() == null) {
            // No model currently
//            System.out.println("No model available - skipping");
            result = ServingResult.noModel;
        }
        else{
            // Score the model
            long start = System.currentTimeMillis();
            double quality = (double) modelStore.getCurrentModel().score(dataRecord.get());
            long duration = System.currentTimeMillis() - start;
            modelStore.getCurrentServingInfo().update(duration);
//            System.out.println("Calculated quality - " + quality + " in " + duration + "ms");
            result = new ServingResult(quality, duration);
         }
        return KeyValue.pair(key,result);
    }

    @Override
    public void init(ProcessorContext context) {
        modelStore = (ModelStateStore) context.getStateStore(ApplicationKafkaParameters.STORE_NAME);
        Objects.requireNonNull(modelStore, "State store can't be null");

    }

    @Deprecated
    public KeyValue<byte[], ServingResult> punctuate(long timestamp) { return null; }

    @Override
    public void close() {}
}
