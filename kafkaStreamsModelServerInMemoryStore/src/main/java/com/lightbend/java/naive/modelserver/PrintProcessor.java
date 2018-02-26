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
        // Exercise: Provide implementation here.
        // 1. Match on `value.processed`, which returns a Boolean
        // 2. If true, print fields in the value object
        // 1. If false, print that no model is available, so skipping...
    }
}
