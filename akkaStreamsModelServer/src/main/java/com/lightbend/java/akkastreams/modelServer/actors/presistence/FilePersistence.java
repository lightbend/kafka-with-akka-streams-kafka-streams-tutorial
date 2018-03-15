package com.lightbend.java.akkastreams.modelServer.actors.presistence;

import com.lightbend.java.model.DataConverter;
import com.lightbend.java.model.Model;
import com.lightbend.java.model.ModelServingInfo;

import java.io.*;
import java.util.Optional;

public class FilePersistence {

    private final static String  basDir = "persistence";

    private FilePersistence(){}

    public static ExecutionState restoreState(String  dataType){
        Optional<DataInputStream> mayBeStream = getDataInputStream(dataType);
        if(mayBeStream.isPresent())
            return new ExecutionState(DataConverter.readModel(mayBeStream.get()), DataConverter.readServingInfo(mayBeStream.get()));
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

    public static void saveState(String dataType, Model model, ModelServingInfo servingInfo){
        Optional<DataOutputStream> mayBeOutput = getDataOutputStream(dataType);
        if(mayBeOutput.isPresent()) {
            DataConverter.writeModel(model, mayBeOutput.get());
            DataConverter.writeServingInfo(servingInfo, mayBeOutput.get());
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
}
