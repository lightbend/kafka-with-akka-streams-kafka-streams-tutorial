package com.lightbend.queriablestate;

import java.util.Objects;

public class ModelServingInfo {

    private String name;
    private String description;
    private long since;
    private long invocations;
    private double duration;
    private long min;
    private long max;

    public ModelServingInfo(){}

    public ModelServingInfo(final String name, final String description, final long since) {
        this.name = name;
        this.description = description;
        this.since = since;
        this.invocations = 0;
        this.duration = 0.;
        this.min = Long.MAX_VALUE;
        this.max = Long.MIN_VALUE;
    }

    public ModelServingInfo(final String name, final String description, final long since, final long invocations,
                            final double duration, final long min, final long max) {
        this.name = name;
        this.description = description;
        this.since = since;
        this.invocations = invocations;
        this.duration = duration;
        this.min = min;
        this.max = max;
    }

    public void update(long execution){
        invocations++;
        duration += execution;
        if(execution < min) min = execution;
        if(execution > max) max = execution;
    }

    public String getName() {return name;}

    public void setName(String name) {this.name = name;}

    public String getDescription() {return description;}

    public void setDescription(String description) {this.description = description;}

    public long getSince() {return since;}

    public void setSince(long since) {this.since = since;}

    public long getInvocations() {return invocations;}

    public void setInvocations(long invocations) {this.invocations = invocations;}

    public double getDuration() {return duration;}

    public void setDuration(double duration) {this.duration = duration;}

    public long getMin() {return min;}

    public void setMin(long min) {this.min = min;}

    public long getMax() {return max;}

    public void setMax(long max) {this.max = max;}

    @Override
    public String toString() {
        return "ModelServingInfo{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", since=" + since +
                ", invocations=" + invocations +
                ", duration=" + duration +
                ", min=" + min +
                ", max=" + max +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ModelServingInfo that = (ModelServingInfo) o;
        return name.equals(that.name) &&
               description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}