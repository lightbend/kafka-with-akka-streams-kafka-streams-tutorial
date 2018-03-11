package com.lightbend.java.akkastreams.queryablestate.inmemory;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import com.lightbend.java.akkastreams.modelServer.stage.ReadableModelStore;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class RestServiceInMemory extends AllDirectives {

    private static final String host = "localhost";
    private static final int port = 5500;
    private static final Timeout askTimeout = Timeout.apply(5, TimeUnit.SECONDS);

    private ReadableModelStore reader = null;

    // Serve model status: http://localhost:5500/state
    public static void startRest(ActorSystem system, ActorMaterializer materializer, ReadableModelStore reader) {
        RestServiceInMemory resource = new RestServiceInMemory(reader);
        Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = resource.createRoute().flow(system, materializer);
        Http http = Http.get(system);
        CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(host,port), materializer);
        System.out.println("Starting models observer on host " + host + " port " + port);
    }

    public RestServiceInMemory(ReadableModelStore reader){ this.reader = reader; }

    public Route createRoute() {
        return route(
                path("state", () ->
                        get(() -> completeOK(reader.getCurrentServingInfo(), Jackson.marshaller())))
        );
    }

}
