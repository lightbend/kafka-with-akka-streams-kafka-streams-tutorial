package com.lightbend.java.modelserver.modelserver;

import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.Shape;
import com.lightbend.model.ModelWithDescriptor;
import com.lightbend.model.Winerecord;
import scala.collection.JavaConversions;
import scala.collection.immutable.Seq;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ModelStageShape extends Shape {

    Inlet<Winerecord.WineRecord> dataRecordIn;
    Inlet<ModelWithDescriptor> modelRecordIn;
    Outlet<Optional<Double>> scoringResultOut;

    public ModelStageShape(Inlet<Winerecord.WineRecord> dataRecordIn, Inlet<ModelWithDescriptor> modelRecordIn, Outlet<Optional<Double>> scoringResultOut) {
        this.dataRecordIn = dataRecordIn;
        this.modelRecordIn = modelRecordIn;
        this.scoringResultOut = scoringResultOut;
    }


    @Override
    public Seq<Inlet<?>> inlets() {
        List<Inlet<?>> inputs = Arrays.asList(dataRecordIn, modelRecordIn);
        return JavaConversions.asScalaBuffer(inputs).toList();
    }

    @Override
    public Seq<Outlet<?>> outlets() {
        List<Outlet<?>> outputs = Arrays.asList(scoringResultOut);
        return JavaConversions.asScalaBuffer(outputs).toList();
    }

    @Override
    public Shape deepCopy() {
        return new ModelStageShape(dataRecordIn.carbonCopy(), modelRecordIn.carbonCopy(), scoringResultOut);
    }
}
