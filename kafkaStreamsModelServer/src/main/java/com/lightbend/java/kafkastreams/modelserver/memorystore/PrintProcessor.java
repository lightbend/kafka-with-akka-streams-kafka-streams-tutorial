package com.lightbend.java.kafkastreams.modelserver.memorystore;

import com.lightbend.java.model.ServingResult;
import org.apache.kafka.streams.processor.AbstractProcessor;

/**
 * Created by boris on 7/12/17.
 */

public class PrintProcessor extends AbstractProcessor<byte[], ServingResult> {

    @Override
    public void process(byte[] key, ServingResult value) {
        if(value.isProcessed())
            System.out.println("Calculated quality - " + value.getResult() + " in " + value.getDuration() + "ms");
        else
            System.out.println("No model available - skipping");
    }
}
