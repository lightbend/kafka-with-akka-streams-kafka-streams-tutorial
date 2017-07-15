package com.lightbend.model;

import java.io.Serializable;

/**
 * Created by boris on 5/9/17.
 * Basic trait for model
 */
public interface Model extends Serializable {
    Object score(Object input);
    void cleanup();
    byte[] getBytes();
    long getType();
}