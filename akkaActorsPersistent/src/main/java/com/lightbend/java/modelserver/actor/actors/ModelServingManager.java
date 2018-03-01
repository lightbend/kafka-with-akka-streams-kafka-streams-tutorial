package com.lightbend.java.modelserver.actor.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.collect.Lists;
import com.lightbend.java.model.ModelServingInfo;
import com.lightbend.java.model.ModelWithDescriptor;
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
                .match(ModelWithDescriptor.class, modelWithDescriptor -> {
                    ActorRef destination = getModelServer(modelWithDescriptor.getDescriptor().getDataType());
                    destination.forward(modelWithDescriptor, getContext());
                })
                .match(Winerecord.WineRecord.class, dataRecord -> {
                    ActorRef destination = getModelServer(dataRecord.getDataType());
                    destination.forward(dataRecord, getContext());
                })
                .match(GetState.class, dataType -> {
                    Option<ActorRef> mayBeRef = getContext().child(dataType.getDataType());
                    if(mayBeRef.isEmpty())
                        getSender().tell(ModelServingInfo.empty, getSelf());
                    else
                        mayBeRef.get().forward(dataType, getContext());
                    })
                .match(GetModels.class, models -> {
                    getSender().tell(getInstances(), getSelf());
                })
                .build();
    }
}
