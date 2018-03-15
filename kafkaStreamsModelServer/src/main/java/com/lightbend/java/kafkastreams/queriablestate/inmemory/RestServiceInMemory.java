package com.lightbend.java.kafkastreams.queriablestate.inmemory;

import com.lightbend.java.kafkastreams.queriablestate.StoppableService;
import com.lightbend.java.kafkastreams.store.StoreState;
import com.lightbend.java.model.ModelServingInfo;
import org.apache.kafka.streams.KafkaStreams;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *  A simple REST proxy that runs embedded in the Model server. This is used to
 *  demonstrate how a developer can use the Interactive Queries APIs exposed by Kafka Streams to
 *  locate and query the State Stores within a Kafka Streams Application.
 *  https://github.com/confluentinc/examples/blob/3.2.x/kafka-streams/src/main/java/io/confluent/examples/streams/interactivequeries/WordCountInteractiveQueriesRestService.java
 */
@Path("state")
public class RestServiceInMemory implements StoppableService {

    private final KafkaStreams streams;
    private Server jettyServer;
    private StoreState storeState;

    public RestServiceInMemory(final KafkaStreams streams) {
        this.streams = streams;
        storeState = StoreState.getInstance();
    }

    /**
     * Get current value of the of state
     * @return {@link ModelServingInfo} representing the key-value pair
     */
    @GET
    @Path("/value")
    @Produces(MediaType.APPLICATION_JSON)
    public ModelServingInfo servingInfo() {
        return storeState.getCurrentServingInfo();
    }

    /**
     * Start an embedded Jetty Server on the given port
     * @param port    port to run the Server on
     * @throws Exception
     */
    public void start(final int port) throws Exception {

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        jettyServer = new Server(port);
        jettyServer.setHandler(context);

        ResourceConfig rc = new ResourceConfig();
        rc.register(this);
        rc.register(JacksonFeature.class);

        ServletContainer sc = new ServletContainer(rc);
        ServletHolder holder = new ServletHolder(sc);
        context.addServlet(holder, "/*");

        jettyServer.start();
        System.out.println("Starting models observer at " + jettyServer.getURI());
    }

    /**
     * Stop the Jetty Server
     * @throws Exception
     */
    public void stop() throws Exception {
        if (jettyServer != null) {
            jettyServer.stop();
        }
    }

    // surf to http://localhost:8888/state/value
    public static RestServiceInMemory startRestProxy(final KafkaStreams streams, final int port) throws Exception {
        final RestServiceInMemory restService = new RestServiceInMemory(streams);
        restService.start(port);
        return restService;
    }
}
