package com.lightbend.java.model;

import com.lightbend.model.Winerecord;
import java.io.Serializable;

/**
 * Basic trait for a model. For simplicity, we assume the data to be scored are WineRecords.
 * Created by boris on 5/9/17.
 */
public interface Model extends Serializable {
    public Object score(Winerecord.WineRecord record);
    void cleanup();
    byte[] getBytes();
    long getType();
}
