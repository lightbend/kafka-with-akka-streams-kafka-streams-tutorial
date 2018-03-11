package com.lightbend.java.akkastreams.queryablestate.actors;

import akka.NotUsed;
import akka.actor.ActorRef;
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
import com.lightbend.java.akkastreams.modelServer.actors.GetModels;
import com.lightbend.java.akkastreams.modelServer.actors.GetState;
import com.lightbend.java.model.ModelServingInfo;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.server.PathMatchers.segment;
import static akka.pattern.PatternsCS.ask;

public class RestServiceActors extends AllDirectives {

    private static final String host = "localhost";
    private static final int port = 5500;
    private static final Timeout askTimeout = Timeout.apply(5, TimeUnit.SECONDS);

    private ActorRef router = null;

    // See http://localhost:5500/models
    // Then select a model shown and try http://localhost:5500/models, e.g., http://localhost:5500/state/wine

    public static void startRest(ActorSystem system, ActorMaterializer materializer, ActorRef router) {
        RestServiceActors resource = new RestServiceActors(router);
        Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = resource.createRoute().flow(system, materializer);
        Http http = Http.get(system);
        CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(host,port), materializer);
        System.out.println("Starting models observer on host " + host + " port " + port);
    }

    public RestServiceActors(ActorRef router){ this.router = router; }

    public Route createRoute() {
        return route(
                path("models", () ->
                        get(() -> {
                                    CompletionStage<List<String>> models = ask(router, new GetModels(), askTimeout).thenApply(result -> (List<String>)result);
                                    return completeOKWithFuture(models, Jackson.marshaller());
                                }
                        )),
                pathPrefix("state", () ->
                        path(segment(), (String dataType) ->
                                get(() -> {
                                    CompletionStage<ModelServingInfo> info = ask(router, new GetState(dataType), askTimeout).thenApply((ModelServingInfo.class::cast));
                                    return completeOKWithFuture(info, Jackson.marshaller());
                                }))));

    }

}
