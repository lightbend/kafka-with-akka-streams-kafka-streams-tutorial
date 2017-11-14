package com.lightbend.custom.modelserver.store;

import org.apache.kafka.streams.state.StoreBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ModelStateStoreBuilder implements StoreBuilder<ModelStateStore> {

    private final ModelStateStoreSupplier storeSupplier;
    private Map<String, String> logConfig = new HashMap<>();
    boolean enableCaching;
    boolean enableLogging = true;


    public ModelStateStoreBuilder(final ModelStateStoreSupplier storeSupplier) {
        Objects.requireNonNull(storeSupplier, "bytesStoreSupplier can't be null");
        this.storeSupplier = storeSupplier;
    }

    @Override
    public ModelStateStore build() {
        return new ModelStateStore(storeSupplier.name(), enableLogging);
    }

    @Override
    public  ModelStateStoreBuilder withCachingEnabled() {
        enableCaching = true;
        return this;
    }

    @Override
    public  ModelStateStoreBuilder withLoggingEnabled(final Map<String, String> config) {
        Objects.requireNonNull(config, "config can't be null");
        enableLogging = true;
        logConfig = config;
        return this;
    }

    @Override
    public  ModelStateStoreBuilder withLoggingDisabled() {
        enableLogging = false;
        logConfig.clear();
        return this;
    }

    @Override
    public Map<String, String> logConfig() {
        return logConfig;
    }

    @Override
    public boolean loggingEnabled() {
        return enableLogging;
    }

    @Override
    public String name() {
        return storeSupplier.name();
    }

}
