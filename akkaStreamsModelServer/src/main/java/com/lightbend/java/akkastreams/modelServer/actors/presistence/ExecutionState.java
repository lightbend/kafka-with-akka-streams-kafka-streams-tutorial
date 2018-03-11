package com.lightbend.java.akkastreams.modelServer.actors.presistence;

import com.lightbend.java.model.Model;
import com.lightbend.java.model.ModelServingInfo;

import java.util.Optional;

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
