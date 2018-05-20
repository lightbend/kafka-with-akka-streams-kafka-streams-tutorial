package com.lightbend.java.akkastreams.modelserver.actors;

public class GetState {
    private String dataType = null;

    public GetState(String datatype){
        this.dataType = datatype;
    }

    public String getDataType() {
        return dataType;
    }
}
