package com.lightbend.scala.modelServer.model

import java.io.DataInputStream
import java.io.DataOutputStream

import scala.util.control.NonFatal

/* Created by boris on 5/8/17. */
/**
 * State information about the current model being served.
 * Called `ModelServingInfo` in the Java implementation.
 */
final case class ModelToServeStats(
  name: String,
  description: String,
  since: Long,
  usage: Long = 0,
  duration: Double = 0.0,
  min: Long = 0,
  max: Long = 0) {

  def incrementUsage(execution: Long): ModelToServeStats = copy(
    usage = usage + 1,
    duration = duration + execution,
    min = if (execution < min) execution else min,
    max = if (execution > max) execution else max)
}

object ModelToServeStats {
  val empty = ModelToServeStats("None", "None", 0)

  def apply(m: ModelToServe): ModelToServeStats =
    ModelToServeStats(m.name, m.description, System.currentTimeMillis())

  def readServingInfo(input: DataInputStream) : Option[ModelToServeStats] = {
    input.readLong match {
      case length if length > 0 => {
        try {
          Some(ModelToServeStats(input.readUTF, input.readUTF, input.readLong, input.readLong, input.readDouble, input.readLong, input.readLong))
        } catch {
          case NonFatal(e) =>
            System.out.println("Error Deserializing serving info")
            e.printStackTrace()
            None
        }
      }
      case _ => None
    }
  }

  def writeServingInfo(output: DataOutputStream, servingInfo: ModelToServeStats ): Unit = {
    if(servingInfo == null)
      output.writeLong(0)
    else {
      try {
        output.writeLong(5)
        output.writeUTF(servingInfo.description)
        output.writeUTF(servingInfo.name)
        output.writeLong(servingInfo.since)
        output.writeLong(servingInfo.usage)
        output.writeDouble(servingInfo.duration)
        output.writeLong(servingInfo.min)
        output.writeLong(servingInfo.max)
      } catch {
        case NonFatal(e) =>
          System.out.println("Error Serializing servingInfo")
          e.printStackTrace()
      }
    }
  }
}

