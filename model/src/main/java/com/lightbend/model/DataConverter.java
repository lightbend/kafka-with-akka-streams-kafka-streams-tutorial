package com.lightbend.model;

import com.lightbend.model.PMML.PMMLModelFactory;
import com.lightbend.model.tensorflow.TensorflowModelFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by boris on 6/28/17.
 */
public class DataConverter {

    private static final Map<Integer, ModelFactory> factories = new HashMap<Integer, ModelFactory>() {
        {
            put(Modeldescriptor.ModelDescriptor.ModelType.TENSORFLOW.getNumber(), TensorflowModelFactory.getInstance());
            put(Modeldescriptor.ModelDescriptor.ModelType.PMML.getNumber(), PMMLModelFactory.getInstance());
        }
    };

    private DataConverter(){}

    public static Optional<Winerecord.WineRecord> convertData(byte[] binary){
        try {
            // Unmarshall record
            return Optional.of(Winerecord.WineRecord.parseFrom(binary));
        } catch (Throwable t) {
            // Oops
            System.out.println("Exception parsing input record" + new String(binary));
            t.printStackTrace();
            return Optional.empty();
        }
    }

    public static Optional<CurrentModelDescriptor> convertModel(byte[] binary){
        try {
            // Unmarshall record
            Modeldescriptor.ModelDescriptor model = Modeldescriptor.ModelDescriptor.parseFrom(binary);
            // Return it
            if(model.getMessageContentCase().equals(Modeldescriptor.ModelDescriptor.MessageContentCase.DATA)){
               return Optional.of(new CurrentModelDescriptor(
                        model.getName(), model.getDescription(), model.getModeltype(),
                        model.getData().toByteArray(), null, model.getDataType()));
            }
            else {
                System.out.println("Location based model is not yet supported");
                return Optional.empty();
            }
        } catch (Throwable t) {
            // Oops
            System.out.println("Exception parsing input record" + new String(binary));
            t.printStackTrace();
            return Optional.empty();
        }
    }

    public static Optional<ModelWithDescriptor> convertModel(Optional<CurrentModelDescriptor> descriptor){

        CurrentModelDescriptor model = descriptor.get();
        System.out.println("New scoring model " + model);
        if(model.getModelData() == null) {
            System.out.println("Location based model is not yet supported");
            return Optional.empty();
        }
        ModelFactory factory = factories.get(model.getModelType().ordinal());
        if(factory == null){
            System.out.println("Bad model type " + model.getModelType());
            return Optional.empty();
        }
        Optional<Model> current = factory.create(model);
        if(current.isPresent())
            return Optional.of(new ModelWithDescriptor(current.get(),model));
        return Optional.empty();
    }
}