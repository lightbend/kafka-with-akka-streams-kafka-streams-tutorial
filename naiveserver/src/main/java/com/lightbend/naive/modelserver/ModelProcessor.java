package com.lightbend.naive.modelserver;

import com.lightbend.model.CurrentModelDescriptor;
import com.lightbend.model.DataConverter;
import com.lightbend.model.ModelWithDescriptor;
import com.lightbend.naive.modelserver.queriablestate.ModelServingInfo;
import com.lightbend.naive.modelserver.store.StoreState;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.Optional;

/**
 * Created by boris on 7/12/17.
 */

public class ModelProcessor extends AbstractProcessor<byte[], byte[]> {

    private StoreState modelStore;

    @Override
    public void process(byte[] key, byte[] value) {

        Optional<CurrentModelDescriptor> descriptor = DataConverter.convertModel(value);
        if(!descriptor.isPresent()){
            return;                                                 // Bad record
        }
        Optional<ModelWithDescriptor> modelWithDescriptor = DataConverter.convertModel(descriptor);
        if(modelWithDescriptor.isPresent()){
           modelStore.setNewModel(modelWithDescriptor.get().getModel());
           modelStore.setNewServingInfo(new ModelServingInfo(modelWithDescriptor.get().getDescriptor().getName(),
                    modelWithDescriptor.get().getDescriptor().getDescription(), 0));
           return;
        }
    }

    @Override
    public void init(ProcessorContext context) {
        modelStore = StoreState.getInstance();
    }
}
