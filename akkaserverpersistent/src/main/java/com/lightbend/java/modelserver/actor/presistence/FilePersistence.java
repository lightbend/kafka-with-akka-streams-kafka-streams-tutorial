package com.lightbend.java.modelserver.actor.presistence;

import com.lightbend.model.Model;
import com.lightbend.model.ModelFactory;
import com.lightbend.model.ModelServingInfo;
import com.lightbend.model.Modeldescriptor;
import com.lightbend.model.PMML.PMMLModelFactory;
import com.lightbend.model.tensorflow.TensorflowModelFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FilePersistence {

    private final static String  basDir = "persistence";
    private static final Map<Integer, ModelFactory> factories = new HashMap<Integer, ModelFactory>() {
        {
            put(Modeldescriptor.ModelDescriptor.ModelType.TENSORFLOW.getNumber(), TensorflowModelFactory.getInstance());
            put(Modeldescriptor.ModelDescriptor.ModelType.PMML.getNumber(), PMMLModelFactory.getInstance());
        }
    };

    private FilePersistence(){}

    public static ExecutionState restoreState(String  dataType){
        Optional<DataInputStream> mayBeStream = getDataInputStream(dataType);
        if(mayBeStream.isPresent())
            return new ExecutionState(readModel(mayBeStream.get()), readServingInfo(mayBeStream.get()));
        else
            return new ExecutionState();
    }

    private static Optional<DataInputStream> getDataInputStream(String fileName) {
        File file = new File(basDir + "/" + fileName);
        if(file.exists())
            try {
                return Optional.of(new DataInputStream(new FileInputStream(file)));
            }catch (Throwable t){
                System.out.println("Error Deserializing model");
                t.printStackTrace();
                return Optional.empty();
            }
        else
            return Optional.empty();
    }

    private static Optional<Model> readModel(DataInputStream input) {

        try {
            int length = (int)input.readLong();
            if (length == 0)
                return Optional.empty();
            int type = (int) input.readLong();
            byte[] bytes = new byte[length];
            input.read(bytes);
            ModelFactory factory = factories.get(type);
            return Optional.of(factory.restore(bytes));
        } catch (Throwable t) {
            System.out.println("Error Deserializing model");
            t.printStackTrace();
            return Optional.empty();
        }
    }

    private static Optional<ModelServingInfo> readServingInfo(DataInputStream input){
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
            return Optional.of(new ModelServingInfo(name, descriprtion, since, invocations, duration, min, max));
        } catch (Throwable t) {
            System.out.println("Error Deserializing serving info");
            t.printStackTrace();
            return Optional.empty();
        }
    }

    public static void saveState(String dataType, Model model, ModelServingInfo servingInfo){
        Optional<DataOutputStream> mayBeOutput = getDataOutputStream(dataType);
        if(mayBeOutput.isPresent()) {
            writeModel(model, mayBeOutput.get());
            writeServingInfo(servingInfo, mayBeOutput.get());
            try {
                mayBeOutput.get().flush();
                mayBeOutput.get().close();
            }catch (Throwable t){
                System.out.println("Error Deserializing serving info");
                t.printStackTrace();
            }
        }
    }

    private static Optional<DataOutputStream> getDataOutputStream(String fileName){
        try {
            File dir = new File(basDir);
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(dir, fileName);
            if (!file.exists())
                file.createNewFile();
            return Optional.of(new DataOutputStream(new FileOutputStream(file)));
        }
        catch (Throwable t){
            System.out.println("Error Deserializing serving info");
            t.printStackTrace();
            return Optional.empty();
        }
    }

    private static void writeModel(Model model, DataOutputStream output){
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

    private static void writeServingInfo(ModelServingInfo servingInfo, DataOutputStream output){
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
}
