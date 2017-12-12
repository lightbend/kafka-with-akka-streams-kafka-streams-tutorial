package com.lightbend.java.modelserver.modelserver;

import akka.japi.Pair;
import akka.stream.Attributes;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.stage.AbstractGraphStageWithMaterializedValue;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStageLogic;
import com.lightbend.model.Model;
import com.lightbend.model.ModelServingInfo;
import com.lightbend.model.ModelWithDescriptor;
import com.lightbend.model.Winerecord;

import java.util.Optional;

public class ModelStage extends AbstractGraphStageWithMaterializedValue<ModelStageShape, ReadableModelStore> {

    Inlet<Winerecord.WineRecord> dataRecordIn = Inlet.create("dataRecordIn");
    Inlet<ModelWithDescriptor> modelRecordIn = Inlet.create("modelRecordIn");
    Outlet<Optional<Double>> scoringResultOut = Outlet.create("scoringOut");

    Optional<Model> currentModel = Optional.empty();
    Optional<Model> newModel = Optional.empty();
    Optional<ModelServingInfo> currentServingInfo = Optional.empty();
    Optional<ModelServingInfo> newServingInfo = Optional.empty();

    ModelStageShape shape = new ModelStageShape(dataRecordIn, modelRecordIn, scoringResultOut);

    @Override
    public Pair<GraphStageLogic, ReadableModelStore> createLogicAndMaterializedValuePair(Attributes inheritedAttributes) throws Exception {
        GraphStageLogic logic = new GraphStageLogic(shape) {

            @Override
            public void preStart() {
                tryPull(modelRecordIn);
                tryPull(dataRecordIn);
            }

            {
                setHandler(modelRecordIn, new AbstractInHandler() {
                    @Override
                    public void onPush() throws Exception {
                        ModelWithDescriptor modelWithDescriptor = grab(modelRecordIn);
                        newModel = Optional.of(modelWithDescriptor.getModel());
                        newServingInfo = Optional.of(new ModelServingInfo(modelWithDescriptor.getDescriptor().getName(),
                            modelWithDescriptor.getDescriptor().getDescription(), System.currentTimeMillis()));
                        pull(modelRecordIn);
                    }
                });
                setHandler(dataRecordIn, new AbstractInHandler() {
                    @Override
                    public void onPush() throws Exception {
                        Winerecord.WineRecord dataRecord = grab(dataRecordIn);
                        if(newModel.isPresent()){
                            // update the model
                            if(currentModel.isPresent())
                                currentModel.get().cleanup();
                            currentModel = newModel;
                            currentServingInfo = newServingInfo;
                            newModel = Optional.empty();
                        }

                        // Actually score
                        if(currentModel.isPresent()) {
                            // Score the model
                            long start = System.currentTimeMillis();
                            double quality = (double) currentModel.get().score(dataRecord);
                            long duration = System.currentTimeMillis() - start;
                            currentServingInfo.get().update(duration);
                            System.out.println("Calculated quality - " + quality + " in " + duration + "ms");
                            push(scoringResultOut, Optional.of(quality));
                        }
                        else{
                            // No model currently
                            System.out.println("No model available - skipping");
                            push(scoringResultOut, Optional.empty());
                        }
                        pull(dataRecordIn);

                    }
                });
                setHandler(scoringResultOut, new AbstractOutHandler() {
                    @Override
                    public void onPull() throws Exception {
                        // Do nothing
                    }
                 });
            }
        };
        ReadableModelStore readableModelStateStore = new ReadableModelStore() {
            @Override
            public ModelServingInfo getCurrentServingInfo() {
                return currentServingInfo.orElse(new ModelServingInfo());
            }
        };
        return new Pair(logic, readableModelStateStore);
    }

    @Override
    public ModelStageShape shape() {
        return shape;
    }
}
