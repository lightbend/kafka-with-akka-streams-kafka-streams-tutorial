package com.lightbend.scala.naive.modelserver.store

import com.lightbend.modelServer.model._

case class StoreState(var currentModel: Option[Model] = None, var newModel: Option[Model] = None,
                      var currentState: Option[ModelToServeStats] = None, var newState: Option[ModelToServeStats] = None){}

object StoreState{
  val instance = new StoreState()
  def apply(): StoreState = instance
}
