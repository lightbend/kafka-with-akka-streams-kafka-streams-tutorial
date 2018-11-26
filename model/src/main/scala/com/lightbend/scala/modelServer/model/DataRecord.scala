package com.lightbend.scala.modelServer.model

import com.lightbend.model.winerecord.WineRecord

import scala.util.Try

/**
 * Helper for parsing a byte array into a data record.
 * Created by boris on 5/8/17.
 */
object DataRecord {
  // We inject random parsing errors.
  val percentErrors = 5  // 5%
  val rand = new util.Random()

  // Exercise:
  // This implementation assumes `WineRecords`, of course. Can it be made more generic?
  def fromByteArray(message: Array[Byte]): Try[WineRecord] = Try {
    if (rand.nextInt(100) < percentErrors) throw new RuntimeException(s"FAKE parse error")
    else WineRecord.parseFrom(message)
  }
}
