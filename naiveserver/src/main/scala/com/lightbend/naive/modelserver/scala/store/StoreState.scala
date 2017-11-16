package com.lightbend.naive.modelserver.scala.store

import com.lightbend.modelServer.model._

case class StoreState(var currentModel: Option[Model] = None, var newModel: Option[Model] = None,
                      var currentState: Option[ModelToServeStats] = None, var newState: Option[ModelToServeStats] = None){}

object StoreState{
  val instance = new StoreState()
  def apply(): StoreState = instance
}

case class HostStoreInfo (host: String, port: Int, storeNames: Set[String]){
  override def toString: String = s"HostStoreInfo{host= + $host + port=$port storeNames= ${storeNames.mkString(",")}}"
}
