package com.lightbend.modelserver;

import com.lightbend.model.Model;
import com.lightbend.model.PMMLModel;
import com.lightbend.model.TensorModel;
import com.lightbend.modelserver.store.ModelStateStore;
import com.lightbend.queriablestate.ModelServingInfo;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by boris on 7/12/17.
 */
public class ModelProcessorWithStore extends AbstractProcessor<byte[], byte[]> {

    private ModelStateStore modelStore;
    private ProcessorContext context;

    @Override
    public void process(byte[] key, byte[] value) {

        Optional<ModelDescriptor> descriptor = DataConverter.convertModel(value);
        if(!descriptor.isPresent()){
            context().commit();
            return;                                                 // Bad record
        }
        ModelDescriptor model = descriptor.get();
        System.out.println("New scoring model " + model);
        if(model.getModelData() == null) {
            System.out.println("Location based model is not yet supported");
            return;
        }
        try {
            Model current = null;
            switch (model.getModelType()){
                case PMML:
                    current = new PMMLModel(model.getModelData());
                    break;
                case TENSORFLOW:
                    current = new TensorModel(model.getModelData());
                    break;
                case UNRECOGNIZED:
                    System.out.println("Only PMML and Tensorflow models are currently supported");
                    break;
            }
            modelStore.setNewModel(current);
            modelStore.setNewServingInfo(new ModelServingInfo(model.getName(), model.getDescription(), 0));
//            context().commit();
        } catch (Throwable t) {
            System.out.println("Failed to create model");
            t.printStackTrace();
//            context().commit();
        }
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.context.schedule(10000);
        modelStore = (ModelStateStore) this.context.getStateStore("modelStore");
        Objects.requireNonNull(modelStore, "State store can't be null");
    }
}
