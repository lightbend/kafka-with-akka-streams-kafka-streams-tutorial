package com.lightbend.java.kafkastreams.modelserver.standardstore;

import com.lightbend.java.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.java.model.ModelServingInfo;
import com.lightbend.java.model.ModelWithDescriptor;
import com.lightbend.java.kafkastreams.store.StoreState;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by boris on 7/12/17.
 */
public class ModelProcessor extends AbstractProcessor<byte[], Optional<ModelWithDescriptor>> {

    private KeyValueStore<Integer, StoreState> modelStore;

    @Override
    public void process(byte[] key, Optional<ModelWithDescriptor> modelWithDescriptor) {

        StoreState state = modelStore.get(ApplicationKafkaParameters.STORE_ID);
        if(state == null)
            state = new StoreState();
        state.setNewModel(modelWithDescriptor.get().getModel());
        state.setNewServingInfo(new ModelServingInfo(modelWithDescriptor.get().getDescriptor().getName(),
                    modelWithDescriptor.get().getDescriptor().getDescription(), 0));
        modelStore.put(ApplicationKafkaParameters.STORE_ID, state);
        return;
    }

    @Override
    public void init(ProcessorContext context) {
        modelStore = (KeyValueStore<Integer, StoreState>) context.getStateStore(ApplicationKafkaParameters.STORE_NAME);
        Objects.requireNonNull(modelStore, "State store can't be null");
    }
}
