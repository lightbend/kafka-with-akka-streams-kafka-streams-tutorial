package com.lightbend.java.kafkastreams.queriablestate;

/**
 * Used for different services that offer a stop method.
 */
public interface StoppableService {
    void stop() throws Exception;
}
