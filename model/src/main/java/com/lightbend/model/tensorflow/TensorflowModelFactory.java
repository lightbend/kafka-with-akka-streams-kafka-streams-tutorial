package com.lightbend.model.tensorflow;

import com.lightbend.model.Model;
import com.lightbend.model.CurrentModelDescriptor;
import com.lightbend.model.ModelFactory;

import java.util.Optional;

/**
 * Created by boris on 7/15/17.
 */
public class TensorflowModelFactory implements ModelFactory {

    private static TensorflowModelFactory instance = null;

    @Override
    public Optional<Model> create(CurrentModelDescriptor descriptor) {

        try{
            return Optional.of(new TensorflowModel(descriptor.getModelData()));
        }
        catch (Throwable t){
            System.out.println("Exception creating TensorflowModel from " + descriptor);
            t.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Model restore(byte[] bytes) {
        try{
            return new TensorflowModel(bytes);
        }
        catch (Throwable t){
            System.out.println("Exception restoring PMMLModel from ");
            t.printStackTrace();
            return null;
        }
    }

    public static ModelFactory getInstance(){
        if(instance == null)
            instance = new TensorflowModelFactory();
        return instance;
    }

}
