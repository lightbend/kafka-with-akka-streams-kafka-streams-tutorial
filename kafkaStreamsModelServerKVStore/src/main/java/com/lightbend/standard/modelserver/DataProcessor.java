package com.lightbend.standard.modelserver;

import com.lightbend.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.model.Winerecord;
import com.lightbend.model.ModelServingInfo;
import com.lightbend.standard.modelserver.store.StoreState;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by boris on 7/12/17.
 * used
 * https://github.com/bbejeck/kafka-streams/blob/master/src/main/java/bbejeck/processor/stocks/StockSummaryProcessor.java
 */
public class DataProcessor extends AbstractProcessor<byte[], Optional<Winerecord.WineRecord>> {

    private KeyValueStore<Integer, StoreState> modelStore;

    @Override
    public void process(byte[] key, Optional<Winerecord.WineRecord> dataRecord) {

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
        if( state.getCurrentModel() == null) {
            // No model currently
            System.out.println("No model available - skipping");
        }
        else{
            // Score the model
            long start = System.currentTimeMillis();
            double quality = (double)  state.getCurrentModel().score(dataRecord.get());
            long duration = System.currentTimeMillis() - start;
             state.getCurrentServingInfo().update(duration);
            System.out.println("Calculated quality - " + quality + " in " + duration + "ms");
        }
        modelStore.put(ApplicationKafkaParameters.STORE_ID, state);
    }

    @Override
    public void init(ProcessorContext context) {
        modelStore = (KeyValueStore<Integer, StoreState>) context.getStateStore(ApplicationKafkaParameters.STORE_NAME);
        Objects.requireNonNull(modelStore, "State store can't be null");

    }
}