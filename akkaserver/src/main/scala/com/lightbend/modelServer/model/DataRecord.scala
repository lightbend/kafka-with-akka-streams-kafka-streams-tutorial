package com.lightbend.modelServer.model

import com.lightbend.model.winerecord.WineRecord

import scala.util.Try

/**
 * Created by boris on 5/8/17.
 */
object DataRecord {

  def fromByteArray(message: Array[Byte]): Try[WineRecord] = Try {
    WineRecord.parseFrom(message)
  }
}