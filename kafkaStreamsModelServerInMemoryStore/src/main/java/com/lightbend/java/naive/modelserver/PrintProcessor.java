package com.lightbend.java.naive.modelserver;

import com.lightbend.java.model.ServingResult;
import com.lightbend.java.naive.modelserver.store.StoreState;
import org.apache.kafka.streams.processor.AbstractProcessor;

/**
 * Created by boris on 7/12/17.
 */

public class PrintProcessor extends AbstractProcessor<byte[], ServingResult> {

    private StoreState modelStore;

    @Override
    public void process(byte[] key, ServingResult value) {
        if(value.isProcessed())
            System.out.println("Calculated quality - " + value.getResult() + " in " + value.getDuration() + "ms");
        else
            System.out.println("No model available - skipping");
    }
}
