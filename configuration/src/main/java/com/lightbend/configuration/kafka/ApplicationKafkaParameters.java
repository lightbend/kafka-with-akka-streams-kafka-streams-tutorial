package com.lightbend.configuration.kafka;

/**
 * Created by boris on 5/18/17.
 * Set of parameters for running applications
 */
public class ApplicationKafkaParameters {

    private ApplicationKafkaParameters(){}

    public static final String LOCAL_ZOOKEEPER_HOST = "zk-1.zk:2181/dcos-service-kafka";
    public static final String LOCAL_KAFKA_BROKER = "broker.kafka.l4lb.thisdcos.directory:9092"/*"10.2.2.221:1025"*/;

    public static final String DATA_TOPIC = "models_data";
    public static final String MODELS_TOPIC = "models_models";

    public static final String DATA_GROUP = "wineRecordsGroup";
    public static final String MODELS_GROUP = "modelRecordsGroup";

    // InfluxDB 
    public static final String influxDBServer = "http://influx-db.marathon.l4lb.thisdcos.directory"/*"http://10.2.2.187"*/;
    public static final int    influxDBPort = 8086/*13698*/;
    public static final String influxDBUser = "root";
    public static final String influxDBPass = "root";
    public static final String influxDBDatabase = "serving";
    public static final String retentionPolicy = "default";

    // Grafana
    public static final String GrafanaHost = "grafana.marathon.l4lb.thisdcos.directory"/*"10.2.2.198"*/;
    public static final int    GrafanaPort = 3000/*4086*/;
    public static final String GrafanaUser = "admin";
    public static final String GrafanaPass = "admin";


}
