package com.lightbend.custom.modelserver;

import com.lightbend.configuration.kafka.ApplicationKafkaParameters;
import com.lightbend.model.ModelWithDescriptor;
import com.lightbend.custom.modelserver.store.ModelStateStore;
import com.lightbend.model.ModelServingInfo;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by boris on 7/12/17.
 */
public class ModelProcessor extends AbstractProcessor<byte[], Optional<ModelWithDescriptor>> {

    private ModelStateStore modelStore;

    @Override
    public void process(byte[] key, Optional<ModelWithDescriptor> modelWithDescriptor) {

        modelStore.setNewModel(modelWithDescriptor.get().getModel());
        modelStore.setNewServingInfo(new ModelServingInfo(modelWithDescriptor.get().getDescriptor().getName(),
                    modelWithDescriptor.get().getDescriptor().getDescription(), 0));
        return;
    }

    @Override
    public void init(ProcessorContext context) {
        modelStore = (ModelStateStore) context.getStateStore(ApplicationKafkaParameters.STORE_NAME);
        Objects.requireNonNull(modelStore, "State store can't be null");
    }
}
