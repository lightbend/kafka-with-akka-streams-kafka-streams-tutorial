package com.lightbend.scala.modelServer.model

import java.io.{DataInputStream, DataOutputStream}

import com.lightbend.model.modeldescriptor.ModelDescriptor

import scala.collection.Map
import scala.util.Try

/**
 * Created by boris on 5/8/17.
 */
case class ModelToServeStats(
  name: String = "",
  description: String = "",
  since: Long = 0,
  var usage: Long = 0,
  var duration: Double = .0,
  var min: Long = Long.MaxValue,
  var max: Long = Long.MinValue) {

  def this(m: ModelToServe) = this(m.name, m.description, System.currentTimeMillis())

  def incrementUsage(execution: Long): ModelToServeStats = {
    usage = usage + 1
    duration = duration + execution
    if (execution < min) min = execution
    if (execution > max) max = execution
    this
  }
}

object ModelToServeStats {
  val empty = ModelToServeStats("None", "None", 0, 0, .0, 0, 0)

  def readServingInfo(input: DataInputStream) : Option[ModelToServeStats] = {
    input.readLong match {
      case length if length > 0 => {
        try {
          Some(ModelToServeStats(input.readUTF, input.readUTF, input.readLong, input.readLong, input.readDouble, input.readLong, input.readLong))
        } catch {
          case t: Throwable =>
            System.out.println("Error Deserializing serving info")
            t.printStackTrace()
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
        case t: Throwable =>
          System.out.println("Error Serializing servingInfo")
          t.printStackTrace()
      }
    }
  }
}

