package com.lightbend.java.modelserver.modelserver;

import akka.stream.AbstractShape;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.Shape;
import com.lightbend.model.Winerecord;
import com.lightbend.java.model.ModelWithDescriptor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ModelStageShape extends AbstractShape {

    private Inlet<Winerecord.WineRecord> dataRecordIn;
    private Inlet<ModelWithDescriptor> modelRecordIn;
    private Outlet<Optional<Double>> scoringResultOut;
    private List<Inlet<?>> inlets;
    private List<Outlet<?>> outlets;

    public ModelStageShape(Inlet<Winerecord.WineRecord> dataRecordIn, Inlet<ModelWithDescriptor> modelRecordIn, Outlet<Optional<Double>> scoringResultOut) {
        this.dataRecordIn = dataRecordIn;
        this.modelRecordIn = modelRecordIn;
        this.scoringResultOut = scoringResultOut;
        inlets = Arrays.asList(dataRecordIn, modelRecordIn);
        outlets = Arrays.asList(scoringResultOut);
    }

    @Override
    public List<Inlet<?>> allInlets() {
        return inlets;
    }

    @Override
    public List<Outlet<?>> allOutlets() {
        return outlets;
    }

    public Inlet<Winerecord.WineRecord> getDataRecordIn() {
        return dataRecordIn;
    }

    public Inlet<ModelWithDescriptor> getModelRecordIn() {
        return modelRecordIn;
    }

    public Outlet<Optional<Double>> getScoringResultOut() {
        return scoringResultOut;
    }

    @Override
    public Shape deepCopy() {
        return new ModelStageShape(dataRecordIn.carbonCopy(), modelRecordIn.carbonCopy(), scoringResultOut.carbonCopy());
    }
}
