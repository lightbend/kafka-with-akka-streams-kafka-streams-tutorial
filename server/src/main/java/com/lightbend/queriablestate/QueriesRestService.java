package com.lightbend.queriablestate;

import com.lightbend.modelserver.store.ModelStateStore;
import com.lightbend.modelserver.store.ReadableModelStateStore;
import org.apache.kafka.streams.KafkaStreams;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 *  A simple REST proxy that runs embedded in the Model server. This is used to
 *  demonstrate how a developer can use the Interactive Queries APIs exposed by Kafka Streams to
 *  locate and query the State Stores within a Kafka Streams Application.
 *  https://github.com/confluentinc/examples/blob/3.2.x/kafka-streams/src/main/java/io/confluent/examples/streams/interactivequeries/WordCountInteractiveQueriesRestService.java
 */
@Path("state")
public class QueriesRestService {

    private final KafkaStreams streams;
    private final MetadataService metadataService;
    private Server jettyServer;

    public QueriesRestService(final KafkaStreams streams) {
        this.streams = streams;
        this.metadataService = new MetadataService(streams);
    }

    /**
     * Get the metadata for all of the instances of this Kafka Streams application
     * @return List of {@link HostStoreInfo}
     */
    @GET()
    @Path("/instances")
    @Produces(MediaType.APPLICATION_JSON)
    public List<HostStoreInfo> streamsMetadata() {
        return metadataService.streamsMetadata();
    }

    /**
     * Get the metadata for all instances of this Kafka Streams application that currently
     * has the provided store.
     * @param store   The store to locate
     * @return  List of {@link HostStoreInfo}
     */
    @GET()
    @Path("/instances/{storeName}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<HostStoreInfo> streamsMetadataForStore(@PathParam("storeName") String store) {
        return metadataService.streamsMetadataForStore(store);
    }

    /**
     * Get current value of the of state
     * @return {@link ModelServingInfo} representing the key-value pair
     */
    @GET
    @Path("{storeName}/value")
    @Produces(MediaType.APPLICATION_JSON)
    public ModelServingInfo servingInfo(@PathParam("storeName") final String storeName) {
//        return ModelState.getInstance().getCurrentServingInfo();
        // Get the  Store
        final ReadableModelStateStore store = streams.store(storeName, new ModelStateStore.ModelStateStoreType());
        if (store == null) {
            throw new NotFoundException();
        }
        return store.getCurrentServingInfo();
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

}