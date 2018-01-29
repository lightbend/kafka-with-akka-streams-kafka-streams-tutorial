package com.lightbend.java.modelserver.queryablestate;

import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import com.lightbend.java.modelserver.modelserver.ReadableModelStore;

public class QueriesAkkaHTTPResource extends AllDirectives {

    private ReadableModelStore reader;

    public QueriesAkkaHTTPResource(ReadableModelStore reader){
        this.reader = reader;
    }

    public Route createRoute() {
        return route(
                path("state", () ->
                    get(() -> completeOK(reader.getCurrentServingInfo(), Jackson.marshaller())))
        );
    }

}
