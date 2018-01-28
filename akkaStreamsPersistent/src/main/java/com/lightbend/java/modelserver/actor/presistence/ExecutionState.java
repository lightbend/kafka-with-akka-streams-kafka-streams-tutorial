package com.lightbend.java.modelserver.actor.presistence;

import com.lightbend.java.model.Model;
import com.lightbend.java.model.ModelServingInfo;

import java.util.Optional;

public class ExecutionState {

    private Optional<Model> mpdel = Optional.empty();
    private Optional<ModelServingInfo> servingInfo = Optional.empty();

    public ExecutionState(){}

    public ExecutionState(Optional<Model> mpdel, Optional<ModelServingInfo> servingInfo){
        this.mpdel = mpdel;
        this.servingInfo = servingInfo;
    }

    public Optional<Model> getMpdel() {
        return mpdel;
    }

    public Optional<ModelServingInfo> getServingInfo() {
        return servingInfo;
    }
}
