package com.lightbend.java.standard.modelserver;

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.java.model.ModelServingInfo;
import com.lightbend.java.model.ServingResult;
import com.lightbend.java.standard.modelserver.store.StoreState;
import com.lightbend.model.Winerecord;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by boris on 7/12/17.
 * used
 * https://github.com/bbejeck/kafka-streams/blob/master/src/main/java/bbejeck/processor/stocks/StockSummaryProcessor.java
 */
public class DataProcessor implements Transformer<byte[], Optional<Winerecord.WineRecord>, KeyValue<byte[], ServingResult>> {

    private KeyValueStore<Integer, StoreState> modelStore;

    @Override
    public KeyValue<byte[], ServingResult> transform(byte[] key, Optional<Winerecord.WineRecord> dataRecord) {

        StoreState state = modelStore.get(ApplicationKafkaParameters.STORE_ID);
        if(state == null)
            state = new StoreState();
        if(state.getNewModel() != null){
            // update the model
            if(state.getCurrentModel() != null)
                state.getCurrentModel().cleanup();
            state.setCurrentModel(state.getNewModel());
            state.setCurrentServingInfo(new ModelServingInfo( state.getNewServingInfo().getName(),
                     state.getNewServingInfo().getDescription(), System.currentTimeMillis()));
            state.setNewServingInfo(null);
            state.setNewModel(null);
        }
        // Actually score
        ServingResult result;
        if( state.getCurrentModel() == null) {
            // No model currently
//            System.out.println("No model available - skipping");
            result = ServingResult.noModel;
        }
        else{
            // Score the model
            long start = System.currentTimeMillis();
            double quality = (double)  state.getCurrentModel().score(dataRecord.get());
            long duration = System.currentTimeMillis() - start;
             state.getCurrentServingInfo().update(duration);
//            System.out.println("Calculated quality - " + quality + " in " + duration + "ms");
            result = new ServingResult(quality, duration);
        }
        modelStore.put(ApplicationKafkaParameters.STORE_ID, state);
        return KeyValue.pair(key,result);
    }

    @Override
    public void init(ProcessorContext context) {
        modelStore = (KeyValueStore<Integer, StoreState>) context.getStateStore(ApplicationKafkaParameters.STORE_NAME);
        Objects.requireNonNull(modelStore, "State store can't be null");
    }

    @Deprecated
    public KeyValue<byte[], ServingResult> punctuate(long timestamp) { return null; }

    @Override
    public void close() {}
}
