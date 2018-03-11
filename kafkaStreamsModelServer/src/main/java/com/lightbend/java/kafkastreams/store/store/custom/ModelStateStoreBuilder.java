package com.lightbend.java.kafkastreams.store.store.custom;

import org.apache.kafka.streams.state.StoreBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ModelStateStoreBuilder implements StoreBuilder<ModelStateStore> {

    private final String name;

    private Map<String, String> logConfig = new HashMap<>();
    boolean enableCaching;
    boolean enableLogging = true;


    public ModelStateStoreBuilder(final String name) {
        Objects.requireNonNull(name, "name can't be null");
        this.name = name;
    }

    @Override
    public ModelStateStore build() {
        return new ModelStateStore(name(), enableLogging);
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
        return name;
    }

}
