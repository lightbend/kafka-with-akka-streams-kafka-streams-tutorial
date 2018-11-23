package com.lightbend.scala.modelServer.model

import com.lightbend.model.winerecord.WineRecord

import scala.util.Try

/**
 * Helper for parsing a byte array into a data record.
 * Created by boris on 5/8/17.
 */
object DataRecord {

  // Exercise:
  // This implementation assumes `WineRecords`, of course. Can it be made more generic?
  def fromByteArray(message: Array[Byte]): Try[WineRecord] = Try {
    WineRecord.parseFrom(message)
  }
}
