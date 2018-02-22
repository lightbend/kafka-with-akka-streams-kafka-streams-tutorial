package com.lightbend.java.model;

import com.lightbend.model.Winerecord;
import java.io.Serializable;

/**
 * Created by boris on 5/9/17.
 * Basic trait for model
 */
public interface Model extends Serializable {
    public Object score(Winerecord.WineRecord record);
    void cleanup();
    byte[] getBytes();
    long getType();
}
