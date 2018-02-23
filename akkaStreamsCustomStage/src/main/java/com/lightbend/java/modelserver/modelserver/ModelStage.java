package com.lightbend.java.modelserver.modelserver;

import akka.japi.Pair;
import akka.japi.function.Procedure;
import akka.stream.Attributes;
import akka.stream.FlowShape;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.stage.*;
import com.lightbend.java.model.Model;
import com.lightbend.java.model.ModelServingInfo;
import com.lightbend.java.model.ModelWithDescriptor;
import com.lightbend.java.model.ServingResult;
import com.lightbend.model.Winerecord;

import java.util.Optional;

public class ModelStage extends AbstractGraphStageWithMaterializedValue<FlowShape<Winerecord.WineRecord, ServingResult>, ReadableModelStore> {

    Inlet<Winerecord.WineRecord> dataRecordIn = Inlet.create("dataRecordIn");
    Outlet<ServingResult> scoringResultOut = Outlet.create("scoringOut");
    private final FlowShape<Winerecord.WineRecord, ServingResult> shape = FlowShape.of(dataRecordIn, scoringResultOut);

    @Override
    public FlowShape<Winerecord.WineRecord, ServingResult> shape() { return shape; }

    private class ModelLogic extends GraphStageLogicWithLogging{
        // state must be kept in the Logic instance, since it is created per stream materialization
        Optional<Model> currentModel = Optional.empty();
        Optional<Model> newModel = Optional.empty();
        Optional<ModelServingInfo> currentServingInfo = Optional.empty();
        Optional<ModelServingInfo> newServingInfo = Optional.empty();

        public ModelLogic(FlowShape<Winerecord.WineRecord, ServingResult> shape){
            super(shape);
            setHandler(dataRecordIn, new AbstractInHandler() {
                @Override
                public void onPush() throws Exception {
                    Winerecord.WineRecord dataRecord = grab(dataRecordIn);
                    if (newModel.isPresent()) {
                        // update the model
                        if (currentModel.isPresent())
                            currentModel.get().cleanup();
                        currentModel = newModel;
                        currentServingInfo = newServingInfo;
                        newModel = Optional.empty();
                    }

                    // Actually score
                    if (currentModel.isPresent()) {
                        // Score the model
                        long start = System.currentTimeMillis();
                        double quality = (double) currentModel.get().score(dataRecord);
                        long duration = System.currentTimeMillis() - start;
                        currentServingInfo.get().update(duration);
//                        System.out.println("Calculated quality - " + quality + " in " + duration + "ms");
                        push(scoringResultOut, new ServingResult(quality, duration));
                    } else {
                        // No model currently
//                        System.out.println("No model available - skipping");
                        push(scoringResultOut, ServingResult.noModel);
                    }
                }
            });
            setHandler(scoringResultOut, new AbstractOutHandler() {
                @Override
                public void onPull() throws Exception {
                    pull(dataRecordIn);
                }
            });
        }

        AsyncCallback<ModelWithDescriptor> callback = createAsyncCallback(new Procedure<ModelWithDescriptor>() {
            @Override
            public void apply(ModelWithDescriptor model) throws Exception {
                System.out.println("Updated model: " + model);
                newModel = Optional.of(model.getModel());
                newServingInfo = Optional.of(new ModelServingInfo(model.getDescriptor().getName(),
                        model.getDescriptor().getDescription(), System.currentTimeMillis()));
            }
        });
    }

    @Override
    public Pair<GraphStageLogic, ReadableModelStore> createLogicAndMaterializedValuePair(Attributes inheritedAttributes) throws Exception {
        ModelLogic logic = new ModelLogic(shape);

        // we materialize this value so whoever runs the stream can get the current serving info
        ReadableModelStore modelStateStore = new ReadableModelStore() {
            @Override
            public ModelServingInfo getCurrentServingInfo() {
                return logic.currentServingInfo.orElse(ModelServingInfo.empty);
            }

            @Override
            public void setModel(ModelWithDescriptor model) {
                logic.callback.invoke(model);
            }
        };
        return new Pair<>(logic, modelStateStore);
    }
}
