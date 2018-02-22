package com.lightbend.java.model;

public class ServingResult {

    public static ServingResult noModel = new ServingResult();

    private boolean processed;
    private double result;
    private long duration;

    public ServingResult(){
        processed = false;
        result = 0.;
        duration = 0;
    }
    public ServingResult(double result, long duration){
        processed = true;
        this.result = result;
        this.duration = duration;
    }

    public boolean isProcessed() {
        return processed;
    }

    public double getResult() {
        return result;
    }

    public long getDuration() {
        return duration;
    }
}
