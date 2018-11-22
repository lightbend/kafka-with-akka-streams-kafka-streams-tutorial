package com.lightbend.java.model;

import java.util.Optional;

/* Created by boris on 7/14/17. */

/**
 * Basic trait for a model factory.
 */
public interface ModelFactory {
    Optional<Model> create(CurrentModelDescriptor descriptor);
    Model restore(byte[] bytes);
}
