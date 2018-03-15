package com.lightbend.java.kafkastreams.store.store;

import com.lightbend.java.kafkastreams.store.StoreState;
import com.lightbend.model.Modeldescriptor.ModelDescriptor;
import com.lightbend.java.model.DataConverter;
import com.lightbend.java.model.Model;
import com.lightbend.java.model.ModelFactory;
import com.lightbend.java.model.ModelServingInfo;
import com.lightbend.java.model.PMML.PMMLModelFactory;
import com.lightbend.java.model.tensorflow.TensorflowModelFactory;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by boris on 7/11/17.
 * based on
 * https://github.com/confluentinc/examples/blob/3.2.x/kafka-streams/src/main/scala/io/confluent/examples/streams/algebird/TopCMSSerde.scala
 */
public class ModelStateSerde implements Serde<StoreState> {

    final private Serializer<StoreState> serializer;
    final private Deserializer<StoreState> deserializer;

    public ModelStateSerde(){
        serializer = new ModelStateSerializer();
        deserializer = new ModelStateDeserializer();
    }

    @Override public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override public void close() {}

    @Override public Serializer<StoreState> serializer() {
        return serializer;
    }

    @Override public Deserializer<StoreState> deserializer() {
        return deserializer;
    }

    public static class ModelStateSerializer implements Serializer<StoreState> {

        private ByteArrayOutputStream bos = new ByteArrayOutputStream();


        @Override public void configure(Map<String, ?> configs, boolean isKey) {}

        @Override public byte[] serialize(String topic, StoreState state) {

//            System.out.println("Serializing Store !!");

            bos.reset();
            DataOutputStream output = new DataOutputStream(bos);

            DataConverter.writeModel(state.getCurrentModel(), output);
            DataConverter.writeModel(state.getNewModel(), output);

            DataConverter.writeServingInfo(state.getCurrentServingInfo(), output);
            DataConverter.writeServingInfo(state.getNewServingInfo(), output);

            try {
                output.flush();
                output.close();
            }
            catch(Throwable t){}
            return bos.toByteArray();

        }

        @Override public void close() {}
    }
    public static class ModelStateDeserializer implements Deserializer<StoreState> {

        private static final Map<Integer, ModelFactory> factories = new HashMap<Integer, ModelFactory>() {
            {
                put(ModelDescriptor.ModelType.TENSORFLOW.getNumber(), TensorflowModelFactory.getInstance());
                put(ModelDescriptor.ModelType.PMML.getNumber(), PMMLModelFactory.getInstance());
            }
        };

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public StoreState deserialize(String topic, byte[] data) {

//            System.out.println("Deserializing Store !!");
            if(data != null) {
                ByteArrayInputStream bis = new ByteArrayInputStream(data);
                DataInputStream input = new DataInputStream(bis);

                Model currentModel = DataConverter.readModel(input).orElse(null);
                Model newModel = DataConverter.readModel(input).orElse(null);

                ModelServingInfo currentServingInfo = DataConverter.readServingInfo(input).orElse(null);
                ModelServingInfo newServingInfo = DataConverter.readServingInfo(input).orElse(null);

                return new StoreState(currentModel, newModel, currentServingInfo, newServingInfo);
            }
            else return new StoreState();
        }

        @Override
        public void close() {
        }
    }
}
