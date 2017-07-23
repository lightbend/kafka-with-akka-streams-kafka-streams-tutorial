package com.lightbend.modelserver.store;

import com.lightbend.model.Model;
import com.lightbend.queriablestate.ModelServingInfo;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.StateRestoreCallback;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.state.QueryableStoreType;
import org.apache.kafka.streams.state.StateSerdes;
import org.apache.kafka.streams.state.internals.StateStoreProvider;

/**
 * Created by boris on 7/11/17.
 * Implementation of a custom state store based on
 * http://docs.confluent.io/current/streams/developer-guide.html#streams-developer-guide-state-store-custom
 * and example at:
 * https://github.com/confluentinc/examples/blob/3.2.x/kafka-streams/src/main/scala/io/confluent/examples/streams/algebird/CMSStore.scala
 *
 */
public class ModelStateStore implements StateStore, ReadableModelStateStore {

    private String name = null;
    private boolean loggingEnabled = false;
    private ModelStateStoreChangeLogger changeLogger = null;
    /**
     * The "storage backend" of this store.
     * Needs proper initializing in case the store's changelog is empty.
     */
    private StoreState state = null;
    private boolean open = false;
    private int changelogKey = 42;

    /*
     * @param name            The name of this store instance
     */

    public ModelStateStore(String name, boolean loggingEnabled) {
        this.name = name;
        this.loggingEnabled = loggingEnabled;
        state = new StoreState();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void init(ProcessorContext context, StateStore root) {
        StateSerdes<Integer, StoreState> serdes = new StateSerdes<Integer, StoreState>(
                name, Serdes.Integer(), new ModelStateSerde());
        changeLogger = new ModelStateStoreChangeLogger(name, context, serdes);
        if (root != null && loggingEnabled) {
            context.register(root, loggingEnabled, new StateRestoreCallback() {
                @Override
                public void restore(byte[] key, byte[] value) {
                    if (value == null) {
                        state.zero();
                    } else {
                        state = serdes.valueFrom(value);
                    }
                }
            });
        }
        open = true;
    }

    /**
     * Periodically saves the latest state to Kafka.
     * =Implementation detail=
     * The changelog records have the form: (hardcodedKey, StoreState).  That is, we are backing up the
     * underlying StoreState data structure in its entirety to Kafka.
     */
    @Override
    public void flush() {
        if (loggingEnabled) {
            changeLogger.logChange(changelogKey, state);
        }
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public boolean persistent() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    public Model getCurrentModel() {
        return state.getCurrentModel();
    }

    public void setCurrentModel(Model currentModel) {
        state.setCurrentModel(currentModel);
    }

    public Model getNewModel() {
        return state.getNewModel();
    }

    public void setNewModel(Model newModel) {
        state.setNewModel(newModel);
    }

    public ModelServingInfo getCurrentServingInfo() {
        return state.getCurrentServingInfo();
    }

    public void setCurrentServingInfo(ModelServingInfo currentServingInfo) {
        state.setCurrentServingInfo(currentServingInfo);
    }

    public ModelServingInfo getNewServingInfo() {
        return state.getNewServingInfo();
    }

    public void setNewServingInfo(ModelServingInfo newServingInfo) {
        state.setNewServingInfo(newServingInfo);
    }

    public static class ModelStateStoreType implements QueryableStoreType<ReadableModelStateStore> {

        @Override
        public boolean accepts(StateStore stateStore) {
            return stateStore instanceof ModelStateStore;
        }

        @Override
        public ReadableModelStateStore create(StateStoreProvider provider, String storeName) {
            return provider.stores(storeName, this).get(0);
        }

    }
}
