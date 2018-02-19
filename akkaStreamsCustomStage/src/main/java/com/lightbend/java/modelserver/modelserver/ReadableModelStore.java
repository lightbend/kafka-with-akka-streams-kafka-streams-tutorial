package com.lightbend.java.modelserver.modelserver;

import com.lightbend.java.model.ModelServingInfo;
import com.lightbend.java.model.ModelWithDescriptor;

public interface  ReadableModelStore {
    public ModelServingInfo getCurrentServingInfo();

    public void setModel(ModelWithDescriptor model);
}