package com.lightbend.influxdb;

import com.lightbend.configuration.kafka.ApplicationKafkaParameters;
import org.apache.commons.codec.binary.Base64;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;


public class InfluxDBClient {

    private InfluxDB influxDB;
    private static String dsfile = "/grafana-source.json";
    private static String dashfile = "/grafana-dashboard.json";

    public InfluxDBClient() {
        influxDB = InfluxDBFactory.connect(ApplicationKafkaParameters.influxDBServer + ":" + ApplicationKafkaParameters.influxDBPort,
                ApplicationKafkaParameters.influxDBUser, ApplicationKafkaParameters.influxDBPass);
        if(!influxDB.databaseExists(ApplicationKafkaParameters.influxDBDatabase)){
            influxDB.createDatabase(ApplicationKafkaParameters.influxDBDatabase);
            influxDB.dropRetentionPolicy("autogen", ApplicationKafkaParameters.influxDBDatabase);
            influxDB.createRetentionPolicy(ApplicationKafkaParameters.retentionPolicy, ApplicationKafkaParameters.influxDBDatabase,
                    "1d", "30m", 1, true);
        }

        influxDB.setDatabase(ApplicationKafkaParameters.influxDBDatabase);
        // Flush every 2000 Points, at least every 100ms
        influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS);
        // set retention policy
        influxDB.setRetentionPolicy(ApplicationKafkaParameters.retentionPolicy);

        // Make sure Grafana is set up
        String authString = ApplicationKafkaParameters.GrafanaUser + ":" + ApplicationKafkaParameters.GrafanaPass;
        String authStringEnc = new String(Base64.encodeBase64(authString.getBytes()));

        try {
            // Source
            String source = "http://" + ApplicationKafkaParameters.GrafanaHost + ":" + ApplicationKafkaParameters.GrafanaPort + "/api/datasources";
            URL url = new URL(source);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            // Setting basic post request
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "Basic " + authStringEnc);

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            streamData(this.getClass().getResourceAsStream(dsfile), wr);
            System.out.println("Uploaded Grafana source");
            printResponce(con);

            String dashboard = "http://" + ApplicationKafkaParameters.GrafanaHost + ":" + ApplicationKafkaParameters.GrafanaPort + "/api/dashboards/db";
            url = new URL(dashboard);
            con = (HttpURLConnection) url.openConnection();

            // Setting basic post request
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "Basic " + authStringEnc);

            // Send post request
            con.setDoOutput(true);
            wr = new DataOutputStream(con.getOutputStream());
            streamData(this.getClass().getResourceAsStream(dashfile), wr);
            System.out.println("Uploaded Grafana dashboard");
            printResponce(con);

        }
        catch (Throwable t){}
    }

    private static void printResponce(HttpURLConnection con){
        try {
            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String output;
            StringBuffer response = new StringBuffer();

            while ((output = in.readLine()) != null) {
                response.append(output);
            }
            in.close();

            //printing result from response
            System.out.println("Response message" + response.toString());
        }
        catch (Throwable t){}
    }

    private static void streamData(InputStream in, DataOutputStream out){
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
            out.close();
        }
        catch (Throwable t){}
    }

    public void writePoint(String engine, String model, double calculated, double duration){
        Point point = Point.measurement("serving")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("calculated", calculated)
                .addField("duration", duration)
                .tag("engine", engine)
                .tag("model", model)
                .build();
        influxDB.write(point);
    }
}