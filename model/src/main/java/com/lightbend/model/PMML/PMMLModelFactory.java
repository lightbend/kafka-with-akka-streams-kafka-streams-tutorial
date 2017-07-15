package com.lightbend.model.PMML;

import com.lightbend.model.Model;
import com.lightbend.model.CurrentModelDescriptor;
import com.lightbend.model.ModelFactory;

import java.util.Optional;

/**
 * Created by boris on 7/15/17.
 */
public class PMMLModelFactory implements ModelFactory {

    private static ModelFactory instance = null;

    private PMMLModelFactory(){}

    @Override
    public Optional<Model> create(CurrentModelDescriptor descriptor) {
        try{
            return Optional.of(new PMMLModel(descriptor.getModelData()));
        }
        catch (Throwable t){
            System.out.println("Exception creating PMMLModel from " + descriptor);
            t.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Model restore(byte[] bytes) {
        try{
            return new PMMLModel(bytes);
        }
        catch (Throwable t){
            System.out.println("Exception restoring PMMLModel from ");
            t.printStackTrace();
            return null;
        }
    }

    public static ModelFactory getInstance(){
        if(instance == null)
            instance = new PMMLModelFactory();
        return instance;
    }
}
