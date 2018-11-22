package com.lightbend.java.model;

/**
 * Used as a message to an Actor with data for a new model instance.
 */
public class ModelWithDescriptor {

    private Model model;
    private CurrentModelDescriptor descriptor;

    public ModelWithDescriptor(Model model, CurrentModelDescriptor descriptor){
        this.model = model;
        this.descriptor = descriptor;
    }

    public Model getModel() {
        return model;
    }

    public CurrentModelDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String toString() {
        return "ModelWithDescriptor{" +
                "model=" + model +
                ", descriptor=" + descriptor +
                '}';
    }
}
