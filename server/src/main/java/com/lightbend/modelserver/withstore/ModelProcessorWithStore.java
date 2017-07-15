package com.lightbend.modelserver.withstore;

import com.lightbend.model.*;
import com.lightbend.model.PMML.PMMLModelFactory;
import com.lightbend.model.tensorflow.TensorflowModelFactory;
import com.lightbend.modelserver.store.ModelStateStore;
import com.lightbend.queriablestate.ModelServingInfo;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by boris on 7/12/17.
 */
public class ModelProcessorWithStore extends AbstractProcessor<byte[], byte[]> {

    private static final Map<Integer, ModelFactory> factories = new HashMap<Integer, ModelFactory>() {
        {
            put(Modeldescriptor.ModelDescriptor.ModelType.TENSORFLOW.getNumber(), TensorflowModelFactory.getInstance());
            put(Modeldescriptor.ModelDescriptor.ModelType.PMML.getNumber(), PMMLModelFactory.getInstance());
        }
    };
    private ModelStateStore modelStore;
    private ProcessorContext context;

    @Override
    public void process(byte[] key, byte[] value) {

        Optional<CurrentModelDescriptor> descriptor = DataConverter.convertModel(value);
        if(!descriptor.isPresent()){
            return;                                                 // Bad record
        }
        CurrentModelDescriptor model = descriptor.get();
        System.out.println("New scoring model " + model);
        if(model.getModelData() == null) {
            System.out.println("Location based model is not yet supported");
            return;
        }
        ModelFactory factory = factories.get(model.getModelType().ordinal());
        if(factory == null){
            System.out.println("Bad model type " + model.getModelType());
            return;
        }
        Optional<Model> current = factory.create(model);
        if(current.isPresent()) {
            modelStore.setNewModel(current.get());
            modelStore.setNewServingInfo(new ModelServingInfo(model.getName(), model.getDescription(), 0));
            return;
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
