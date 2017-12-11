package com.lightbend.java.modelserver.modelserver;

import com.lightbend.model.ModelServingInfo;

public interface  ReadableModelStore {
    public ModelServingInfo getCurrentServingInfo();
}
