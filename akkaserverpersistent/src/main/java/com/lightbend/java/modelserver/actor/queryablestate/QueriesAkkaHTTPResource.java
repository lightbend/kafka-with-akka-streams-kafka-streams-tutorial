package com.lightbend.java.modelserver.actor.queryablestate;

import akka.actor.ActorRef;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.util.Timeout;
import com.lightbend.java.modelserver.actor.actors.GetModels;
import com.lightbend.java.modelserver.actor.actors.GetState;
import com.lightbend.model.ModelServingInfo;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.server.PathMatchers.segment;
import static akka.pattern.PatternsCS.ask;

public class QueriesAkkaHTTPResource extends AllDirectives {

    final Timeout askTimeout = Timeout.apply(5, TimeUnit.SECONDS);
    ActorRef router = null;

    public QueriesAkkaHTTPResource(ActorRef router){
        this.router = router;
    }

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
