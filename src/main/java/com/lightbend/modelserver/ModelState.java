package com.lightbend.modelserver;

import com.lightbend.model.Model;
import com.lightbend.model.PMMLModel;
import com.lightbend.model.TensorModel;
import com.lightbend.model.Winerecord;
import com.lightbend.queriablestate.ModelServingInfo;

import java.util.Optional;

/**
 * Created by boris on 6/28/17.
 */
public class ModelState {

    private static Model currentModel = null;
    private static Model newModel = null;
    private static ModelServingInfo currentServingInfo = null;
    private static ModelServingInfo newServingInfo = null;

    private static ModelState instance = null;

    private ModelState(){                       // Disallow creation
        currentModel = null;
        newModel = null;
        currentServingInfo = null;
        newServingInfo = null;
    }

    static public synchronized ModelState getInstance(){
        if(instance == null){
            instance = new ModelState();
        }
        return  instance;
    }

    public void updateModel(ModelDescriptor model){
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
            newModel = current;
            newServingInfo = new ModelServingInfo(model.getName(), model.getDescription(), 0);
            return;
        } catch (Throwable t) {
            System.out.println("Failed to create model");
            t.printStackTrace();
            return;
        }

    }

    public Optional<Double> serve(Winerecord.WineRecord data){
        if(newModel != null){
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
            return Optional.empty();
        }
        else{
            // Score the model
            long start = System.currentTimeMillis();
            double quality = (double) currentModel.score(data);
            long duration = System.currentTimeMillis() - start;
            currentServingInfo.update(duration);
            System.out.println("Calculated quality - " + quality + " in " + duration + "ms");
            return Optional.of(quality);
        }
    }

    public ModelServingInfo getCurrentServingInfo() {
        return currentServingInfo;
    }
}