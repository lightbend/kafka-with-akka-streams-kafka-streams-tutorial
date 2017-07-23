package com.lightbend.modelserver.store;

import com.lightbend.model.Model;
import com.lightbend.queriablestate.ModelServingInfo;

/**
 * Created by boris on 7/18/17.
 */
public class StoreState {
    private Model currentModel = null;
    private Model newModel = null;
    private ModelServingInfo currentServingInfo = null;
    private ModelServingInfo newServingInfo = null;

    public StoreState() {
        currentModel = null;
        newModel = null;
        currentServingInfo = null;
        newServingInfo = null;
    }

    public StoreState(Model currentModel, Model newModel, ModelServingInfo currentServingInfo, ModelServingInfo newServingInfo) {
        this.currentModel = currentModel;
        this.newModel = newModel;
        this.currentServingInfo = currentServingInfo;
        this.newServingInfo = newServingInfo;
    }

    public void zero() {
        currentModel = null;
        newModel = null;
        currentServingInfo = null;
        newServingInfo = null;
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
}
