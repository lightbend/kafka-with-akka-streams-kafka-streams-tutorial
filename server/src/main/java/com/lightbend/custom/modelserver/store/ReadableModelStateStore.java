package com.lightbend.custom.modelserver.store;

import com.lightbend.custom.queriablestate.ModelServingInfo;

/**
 * Created by boris on 7/13/17.
 */
public interface ReadableModelStateStore {
    ModelServingInfo getCurrentServingInfo();
}

