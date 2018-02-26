package com.lightbend.java.modelserver.actor.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.collect.Lists;
import com.lightbend.model.Winerecord;
import scala.Option;

import java.util.LinkedList;
import java.util.List;

public class ModelServingManager extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private ActorRef getModelServer(String dataType){

        ActorContext context = getContext();
        Option<ActorRef> mayBeRef = getContext().child(dataType);
        return  mayBeRef.isEmpty() ? context.actorOf(ModelServingActor.props(dataType), dataType) : mayBeRef.get();
    }

    private List<String> getInstances() {

        List<String> names = new LinkedList<>();
        List<ActorRef> children = Lists.newArrayList(getContext().getChildren());
        for(ActorRef ref : children)
            names.add(ref.path().name());
        return names;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                // Exercise: Provide implementation here.
                // Forward request ModelWithDescriptor to the appropriate instance (record.dataType) of the model server
                // 1. Get the model server, by passing the `model.descriptor.dataType`.
                // 2. It's an actor, so `forward` the model to it
                // NOTE: You may need to add imports to complete these exercises.
                .match(Winerecord.WineRecord.class, dataRecord -> {
                    ActorRef destination = getModelServer(dataRecord.getDataType());
                    destination.forward(dataRecord, getContext());
                })
                // Exercise: Provide implementation here.
                // For the message GetState
                // If the actor getState.dataType exists -> forward a request to it.
                // Otherwise return an empty ModelToServeStats:
                // 1. Use the actor context to get the child for the state (`getState.dataType`)
                // 2. Match on the returned value, which will be an Option[ActorRef].
                // 3. If a Some(ref), forward the state to the ref
                // 4. Otherwise, send the empty `ModelToServeStats` as a message to the `sender`.
                 .match(GetModels.class, models -> {
                    getSender().tell(getInstances(), getSelf());
                })
                .build();
    }
}
