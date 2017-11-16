package com.lightbend.modelServer.model

/**
 * Created by boris on 5/9/17.
 * Basic trait for model factory
 */
trait ModelFactory {
  def create(input: ModelToServe): Model
  def restore(bytes: Array[Byte]): Model
}