package com.lightbend.java.naive.modelserver.store;

import com.lightbend.model.Model;
import com.lightbend.model.ModelServingInfo;

/**
 * Created by boris on 7/18/17.
 */
public class StoreState {
    private Model currentModel = null;
    private Model newModel = null;
    private ModelServingInfo currentServingInfo = null;
    private ModelServingInfo newServingInfo = null;
    private static StoreState current = null;

    private StoreState() {
        currentModel = null;
        newModel = null;
        currentServingInfo = null;
        newServingInfo = null;
    }

    private StoreState(Model currentModel, Model newModel, ModelServingInfo currentServingInfo, ModelServingInfo newServingInfo) {
        this.currentModel = currentModel;
        this.newModel = newModel;
        this.currentServingInfo = currentServingInfo;
        this.newServingInfo = newServingInfo;
    }

    public Model getCurrentModel() {
        return currentModel;
    }

    public void setCurrentModel(Model currentModel) {
        this.currentModel = currentModel;
    }

    public Model getNewModel() {
        return newModel;
    }

    public void setNewModel(Model newModel) {
        this.newModel = newModel;
    }

    public ModelServingInfo getCurrentServingInfo() {
        return currentServingInfo;
    }

    public void setCurrentServingInfo(ModelServingInfo currentServingInfo) {
        this.currentServingInfo = currentServingInfo;
    }

    public ModelServingInfo getNewServingInfo() {
        return newServingInfo;
    }

    public void setNewServingInfo(ModelServingInfo newServingInfo) {
        this.newServingInfo = newServingInfo;
    }

    public static synchronized StoreState getInstance(){
        if(current == null)
            current = new StoreState();
        return current;
    }
}
