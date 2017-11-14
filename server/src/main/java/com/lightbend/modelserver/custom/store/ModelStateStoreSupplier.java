package com.lightbend.modelserver.custom.store;

import org.apache.kafka.streams.state.StoreSupplier;

/**
 * Created by boris on 7/11/17.
 * based on https://github.com/confluentinc/examples/blob/3.2.x/kafka-streams/src/main/scala/io/confluent/examples/streams/algebird/CMSStoreSupplier.scala
 */
public class ModelStateStoreSupplier implements StoreSupplier<ModelStateStore> {

    private String name;

    public ModelStateStoreSupplier(String name){

        this.name = name;
    }

    @Override public String name() {
        return name;
    }

    @Override public ModelStateStore get() {return null;}

    @Override public String metricsScope() { return "ModelStore"; }
}
