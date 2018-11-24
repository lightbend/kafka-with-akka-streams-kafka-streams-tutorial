package com.lightbend.java.akkastreams.modelserver.stage;

import com.lightbend.java.model.ModelServingInfo;
import com.lightbend.java.model.ModelWithDescriptor;

/**
 * Abstraction for model information that is persisted and can be restored from storage.
 */
public interface  ReadableModelStore {
    public ModelServingInfo getCurrentServingInfo();

    public void setModel(ModelWithDescriptor model);
}
