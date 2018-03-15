package com.lightbend.scala.kafkastreams.store

import com.lightbend.scala.modelServer.model._

case class StoreState(var currentModel: Option[Model] = None, var newModel: Option[Model] = None,
                      var currentState: Option[ModelToServeStats] = None, var newState: Option[ModelToServeStats] = None){
  def zero() : Unit = {
    currentModel = None
    currentState = None
    newModel = None
    newState = None
  }
}

object StoreState{
  val noneExistent = StoreState(None, None, None, None)
  private val instance = new StoreState()
  def apply(): StoreState = instance
}

case class HostStoreInfo (host: String, port: Int, storeNames: Seq[String]){
  override def toString: String = s"HostStoreInfo{host= + $host + port=$port storeNames= ${storeNames.mkString(",")}}"
}
