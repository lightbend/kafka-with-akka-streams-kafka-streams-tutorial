package com.lightbend.java.modelserver.modelserver;

import com.lightbend.java.model.ModelServingInfo;

public interface  ReadableModelStore {
    public ModelServingInfo getCurrentServingInfo();
}
