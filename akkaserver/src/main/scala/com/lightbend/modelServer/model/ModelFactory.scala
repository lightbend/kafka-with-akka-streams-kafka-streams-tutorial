package com.lightbend.modelServer.model

import com.lightbend.modelServer.ModelToServe

/**
 * Created by boris on 5/9/17.
 * Basic trait for model factory
 */
trait ModelFactory {
  def create(input: ModelToServe): Option[Model]
  def restore(bytes: Array[Byte]): Model
}