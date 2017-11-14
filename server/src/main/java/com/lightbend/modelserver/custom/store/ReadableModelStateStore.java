package com.lightbend.modelserver.custom.store;

import com.lightbend.queriablestate.ModelServingInfo;

/**
 * Created by boris on 7/13/17.
 */
public interface ReadableModelStateStore {
    ModelServingInfo getCurrentServingInfo();
}

