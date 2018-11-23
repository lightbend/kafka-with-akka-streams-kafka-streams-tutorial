package com.lightbend.java.model.tensorflow;

import com.lightbend.java.model.CurrentModelDescriptor;
import com.lightbend.java.model.Model;
import com.lightbend.java.model.ModelFactory;

import java.util.Optional;

/**
 * A factory for creating TensorFlow-based models.
 * Created by boris on 7/15/17.
 */
public class TensorFlowModelFactory implements ModelFactory {

    private static TensorFlowModelFactory instance = null;

    @Override
    public Optional<Model> create(CurrentModelDescriptor descriptor) {

        try{
            return Optional.of(new TensorFlowModel(descriptor.getModelData()));
        }
        catch (Throwable t){
            System.out.println("Exception creating TensorFlowModel from " + descriptor);
            t.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Model restore(byte[] bytes) {
        try{
            return new TensorFlowModel(bytes);
        }
        catch (Throwable t){
            System.out.println("Exception restoring PMMLModel from ");
            t.printStackTrace();
            return null;
        }
    }

    public static ModelFactory getInstance(){
        if(instance == null)
            instance = new TensorFlowModelFactory();
        return instance;
    }

}
