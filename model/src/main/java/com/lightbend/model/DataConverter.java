package com.lightbend.model;

import java.util.Optional;

/**
 * Created by boris on 6/28/17.
 */
public class DataConverter {

    private DataConverter(){}

    public static Optional<Winerecord.WineRecord> convertData(byte[] binary){
        try {
            // Unmarshall record
            return Optional.of(Winerecord.WineRecord.parseFrom(binary));
        } catch (Throwable t) {
            // Oops
            System.out.println("Exception parsing input record" + new String(binary));
            t.printStackTrace();
            return Optional.empty();
        }
    }

    public static Optional<CurrentModelDescriptor> convertModel(byte[] binary){
        try {
            // Unmarshall record
            Modeldescriptor.ModelDescriptor model = Modeldescriptor.ModelDescriptor.parseFrom(binary);
            // Return it
            if(model.getMessageContentCase().equals(Modeldescriptor.ModelDescriptor.MessageContentCase.DATA)){
               return Optional.of(new CurrentModelDescriptor(
                        model.getName(), model.getDescription(), model.getModeltype(),
                        model.getData().toByteArray(), null, model.getDataType()));
            }
            else {
                System.out.println("Location based model is not yet supported");
                return Optional.empty();
            }
        } catch (Throwable t) {
            // Oops
            System.out.println("Exception parsing input record" + new String(binary));
            t.printStackTrace();
            return Optional.empty();
        }
    }
}
