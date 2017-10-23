package com.lightbend.modelserver.store;

import com.lightbend.model.Model;
import com.lightbend.model.ModelFactory;
import com.lightbend.model.Modeldescriptor;
import com.lightbend.model.PMML.PMMLModelFactory;
import com.lightbend.model.tensorflow.TensorflowModelFactory;
import com.lightbend.queriablestate.ModelServingInfo;
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

            writeModel(state.getCurrentModel(), output);
            writeModel(state.getNewModel(), output);

            writeServingInfo(state.getCurrentServingInfo(), output);
            writeServingInfo(state.getNewServingInfo(), output);

            try {
                output.flush();
                output.close();
            }
            catch(Throwable t){}
            return bos.toByteArray();

        }

        private void writeModel(Model model, DataOutputStream output){
            try{
                if(model == null){
                    output.writeLong(0);
                    return;
                }
                byte[] bytes = model.getBytes();
                output.writeLong(bytes.length);
                output.writeLong(model.getType());
                output.write(bytes);
            }
            catch (Throwable t){
                System.out.println("Error Serializing model");
                t.printStackTrace();
            }
        }

        private void writeServingInfo(ModelServingInfo servingInfo, DataOutputStream output){
            try{
                if(servingInfo == null) {
                    output.writeLong(0);
                    return;
                }
                output.writeLong(5);
                output.writeUTF(servingInfo.getDescription());
                output.writeUTF(servingInfo.getName());
                output.writeDouble(servingInfo.getDuration());
                output.writeLong(servingInfo.getInvocations());
                output.writeLong(servingInfo.getMax());
                output.writeLong(servingInfo.getMin());
                output.writeLong(servingInfo.getSince());
            }
            catch (Throwable t){
                System.out.println("Error Serializing servingInfo");
                t.printStackTrace();
            }
        }

        @Override public void close() {}
    }
    public static class ModelStateDeserializer implements Deserializer<StoreState> {

        private static final Map<Integer, ModelFactory> factories = new HashMap<Integer, ModelFactory>() {
            {
                put(Modeldescriptor.ModelDescriptor.ModelType.TENSORFLOW.getNumber(), TensorflowModelFactory.getInstance());
                put(Modeldescriptor.ModelDescriptor.ModelType.PMML.getNumber(), PMMLModelFactory.getInstance());
            }
        };

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public StoreState deserialize(String topic, byte[] data) {

//            System.out.println("Deserializing Store !!");

            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            DataInputStream input = new DataInputStream(bis);

            Model currentModel = readModel(input);
            Model newModel = readModel(input);

            ModelServingInfo currentServingInfo = readServingInfo(input);
            ModelServingInfo newServingInfo = readServingInfo(input);

            return new StoreState(currentModel, newModel, currentServingInfo, newServingInfo);
        }

        @Override
        public void close() {
        }

        private Model readModel(DataInputStream input) {
            try {
                int length = (int)input.readLong();
                if (length == 0)
                    return null;
                int type = (int) input.readLong();
                byte[] bytes = new byte[length];
                input.read(bytes);
                ModelFactory factory = factories.get(type);
                return factory.restore(bytes);
            } catch (Throwable t) {
                System.out.println("Error Deserializing model");
                t.printStackTrace();
                return null;
            }
        }

        private ModelServingInfo readServingInfo(DataInputStream input) {
            try {
                long length = input.readLong();
                if (length == 0)
                    return null;
                String descriprtion = input.readUTF();
                String name = input.readUTF();
                double duration = input.readDouble();
                long invocations = input.readLong();
                long max  = input.readLong();
                long min = input.readLong();
                long since = input.readLong();
                return new ModelServingInfo(name, descriprtion, since, invocations, duration, min, max);
            } catch (Throwable t) {
                System.out.println("Error Deserializing serving info");
                t.printStackTrace();
                return null;
            }
        }
    }
}
