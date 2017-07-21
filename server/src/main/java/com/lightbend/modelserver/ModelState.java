package com.lightbend.modelserver;

import com.lightbend.model.*;
import com.lightbend.model.PMML.PMMLModelFactory;
import com.lightbend.model.tensorflow.TensorflowModelFactory;
import com.lightbend.queriablestate.ModelServingInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Created by boris on 6/28/17.
 */
public class ModelState {

    private static Model currentModel = null;
    private static Model newModel = null;
    private static ModelServingInfo currentServingInfo = null;
    private static ModelServingInfo newServingInfo = null;

    private static final Map<Integer, ModelFactory> factories = new HashMap<Integer, ModelFactory>() {
        {
            put(Modeldescriptor.ModelDescriptor.ModelType.TENSORFLOW.getNumber(), TensorflowModelFactory.getInstance());
            put(Modeldescriptor.ModelDescriptor.ModelType.PMML.getNumber(), PMMLModelFactory.getInstance());
        }
    };

    ModelState(){                       // Disallow creation
        currentModel = null;
        newModel = null;
        currentServingInfo = null;
        newServingInfo = null;
    }

    public void updateModel(CurrentModelDescriptor model){
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
            newModel = current.get();
            newServingInfo = new ModelServingInfo(model.getName(), model.getDescription(), 0);
            return;
        }
    }

    public OptionalDouble serve(Winerecord.WineRecord data){
        if(newModel != null) {
            // update the model
            if(currentModel != null)
                currentModel.cleanup();
            currentModel = newModel;
            currentServingInfo = new ModelServingInfo(
                    newServingInfo.getName(), newServingInfo.getDescription(), System.currentTimeMillis());
            newServingInfo = null;
            newModel = null;
        }
        // Actually score
        if(currentModel == null) {
            // No model currently
            System.out.println("No model available - skipping");
            return OptionalDouble.empty();
        }
        else{
            // Score the model
            long start = System.currentTimeMillis();
            double quality = (double) currentModel.score(data);
            long duration = System.currentTimeMillis() - start;
            currentServingInfo.update(duration);
            System.out.println("Calculated quality - " + quality + " in " + duration + "ms");
            return OptionalDouble.of(quality);
        }
    }

    public ModelServingInfo getCurrentServingInfo() {
        return currentServingInfo;
    }
}
