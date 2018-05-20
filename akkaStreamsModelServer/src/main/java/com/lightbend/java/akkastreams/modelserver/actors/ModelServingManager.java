package com.lightbend.java.akkastreams.modelserver.actors;

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

/**
 * The Manager has two functions. It creates the actors that manage models and score with them,
 * and it routes messages to them.
 */
public class ModelServingManager extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    /** Return the model serving actor for the given type of data. */
    private ActorRef getModelServer(String dataType){

        // Exercise:
        // Currently, one ActorRef for each kind of data is returned. For model serving,
        // we discussed in the presentation that you might want a set of workers for better
        // scalability through parallelism. The line below,
        //   .match(Winerecord.WineRecord.class, dataRecord -> {
        // is where scoring is invoked. Modify this class to create a set of one or more
        // workers. Choose which one to use for a record randomly, round-robin, or whatever.
        // Add this feature without changing the public API of the class, so it's transparent
        // to users.

        // Exercise:
        // One technique used to improve scoring performance is to score each record with a set
        // of models and then pick the best result. "Best result" could mean a few things:
        // 1. The score includes a confidence level and the result with the highest confidence wins.
        // 2. To met latency requirements, at least one of the models is faster than the latency window,
        //    but less accurate. Once the latency window expires, the fast result is returned if the
        //    slower models haven't returned a result in time.
        // Modify the model management and scoring logic to implement one or both scenarios.

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
