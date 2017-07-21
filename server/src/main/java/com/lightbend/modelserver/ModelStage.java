package com.lightbend.modelserver;

import akka.japi.Pair;
import akka.stream.*;
import akka.stream.stage.*;
import com.lightbend.model.CurrentModelDescriptor;
import com.lightbend.model.Winerecord;
import com.lightbend.modelserver.store.ReadableModelStateStore;
import com.lightbend.queriablestate.ModelServingInfo;
import scala.Tuple2;

import java.util.*;

public class ModelStage extends GraphStageWithMaterializedValue<ModelStage.ModelFanInShape, ReadableModelStateStore> {

  private final ModelFanInShape shape = new ModelFanInShape();

  @Override
  public ModelFanInShape shape() {
    return shape;
  }

  @Override
  public Tuple2<GraphStageLogic, ReadableModelStateStore> createLogicAndMaterializedValue(Attributes inheritedAttributes) throws Exception {
    final ModelState modelState = new ModelState();
    
    final GraphStageLogicWithLogging logic = new GraphStageLogicWithLogging(shape()) {
      @Override
      public void preStart() throws Exception {
        tryPull(shape.modelIn);
        tryPull(shape.wineRecordIn);
      }

      {
        setHandler(shape.modelIn, new AbstractInHandler() {
          @Override
          public void onPush() throws Exception {
            final CurrentModelDescriptor desc = grab(shape.modelIn);
            modelState.updateModel(desc);

            pull(shape.modelIn);
          }
        });

        setHandler(shape.wineRecordIn, new AbstractInHandler() {
          @Override
          public void onPush() throws Exception {
            final Winerecord.WineRecord wineRecord = grab(shape.wineRecordIn);
            final OptionalDouble quality = modelState.serve(wineRecord);

            push(shape.qualityOut, Pair.create(wineRecord, quality));
          }
        });

        setHandler(shape.qualityOut, new AbstractOutHandler() {
          @Override
          public void onPull() throws Exception {
            pull(shape.wineRecordIn);
          }
        });
      }
    };

    // we materialize this value so whoever runs the stream can get the current serving info
    final ReadableModelStateStore readableModelStateStore = new ReadableModelStateStore() {
      @Override
      public ModelServingInfo getCurrentServingInfo() {
        return modelState.getCurrentServingInfo();
      }
    };

    return new Tuple2<>(logic, readableModelStateStore);
  }


  public static class ModelFanInShape extends AbstractShape {
    
    public final Inlet<Winerecord.WineRecord> wineRecordIn;  
    public final Inlet<CurrentModelDescriptor> modelIn;
    public final Outlet<Pair<Winerecord.WineRecord, OptionalDouble>> qualityOut;

    public ModelFanInShape() {
      wineRecordIn = Inlet.create("wineRecordIn");
      modelIn = Inlet.create("modelDescriptorIn");
      qualityOut = Outlet.create("qualityOut"); // TODO better name?
    }

    public ModelFanInShape(Inlet<Winerecord.WineRecord> wineRecordIn, Inlet<CurrentModelDescriptor> modelIn, Outlet<Pair<Winerecord.WineRecord, OptionalDouble>> qualityOut) {
      this.wineRecordIn = wineRecordIn;
      this.modelIn = modelIn;
      this.qualityOut = qualityOut;
    }

    @Override
    public List<Inlet<?>> allInlets() {
      return Arrays.asList(wineRecordIn, modelIn);
    }

    @Override
    public List<Outlet<?>> allOutlets() {
      return Collections.emptyList();
    }

    @Override
    public Shape deepCopy() {
      return new ModelFanInShape(wineRecordIn.carbonCopy(), modelIn.carbonCopy(), qualityOut);
    }
  } 
}

