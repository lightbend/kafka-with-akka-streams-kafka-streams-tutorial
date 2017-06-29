package com.lightbend.kafka;

/**
 * Created by boris on 5/18/17.
 * Set of parameters for running applications
 */
public class ApplicationKafkaParameters {

    private ApplicationKafkaParameters(){}

    public static final String LOCAL_ZOOKEEPER_HOST = "localhost:2181";
    public static final String LOCAL_KAFKA_BROKER = "localhost:9092";

    public static final String DATA_TOPIC = "mdata";
    public static final String MODELS_TOPIC = "models";

    public static final String DATA_GROUP = "wineRecordsGroup";
    public static final String MODELS_GROUP = "modelRecordsGroup";
}
