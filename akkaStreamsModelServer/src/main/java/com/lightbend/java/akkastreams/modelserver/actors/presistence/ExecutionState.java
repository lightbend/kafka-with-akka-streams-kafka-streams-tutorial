package com.lightbend.java.akkastreams.modelserver.actors.presistence;

import com.lightbend.java.model.Model;
import com.lightbend.java.model.ModelServingInfo;

import java.util.Optional;

/**
 * Holds the state information that we persist for quick recovery.
 * This state optionally includes the current {@link com.lightbend.java.model.Model} and
 * {@link com.lightbend.java.model.ModelServingInfo}.
 */
public class ExecutionState {

    private Optional<Model> model = Optional.empty();
    private Optional<ModelServingInfo> servingInfo = Optional.empty();

    public ExecutionState(){}

    public ExecutionState(Optional<Model> model, Optional<ModelServingInfo> servingInfo){
        this.model = model;
        this.servingInfo = servingInfo;
    }

    public Optional<Model> getModel() {
        return model;
    }

    public Optional<ModelServingInfo> getServingInfo() {
        return servingInfo;
    }
}
