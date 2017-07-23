package com.lightbend.modelserver.store;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.processor.StateStoreSupplier;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by boris on 7/11/17.
 * based on https://github.com/confluentinc/examples/blob/3.2.x/kafka-streams/src/main/scala/io/confluent/examples/streams/algebird/CMSStoreSupplier.scala
 */
public class ModelStateStoreSupplier implements StateStoreSupplier<ModelStateStore> {

    private String name;
    private Serde<StoreState> serde;
    private boolean loggingEnabled;
    private Map<String, String> logConfig;

    public ModelStateStoreSupplier(String name, Serde<StoreState> serde, boolean loggingEnabled, Map<String, String> logConfig){

        this.name = name;
        this.serde = serde;
        this.loggingEnabled = loggingEnabled;
        this.logConfig = logConfig;
    }

    public ModelStateStoreSupplier(String name, Serde<StoreState> serde) {
        this(name, serde, true, new HashMap<>());
    }

    public ModelStateStoreSupplier(String name, Serde<StoreState> serde, boolean loggingEnabled) {
        this(name, serde, loggingEnabled, new HashMap<>());
    }

    @Override public String name() {
        return name;
    }

    @Override public ModelStateStore get() {return new ModelStateStore(name, loggingEnabled);}

    @Override public Map<String, String> logConfig() {
        return logConfig;
    }

    @Override public boolean loggingEnabled() {
        return loggingEnabled;
    }
}
