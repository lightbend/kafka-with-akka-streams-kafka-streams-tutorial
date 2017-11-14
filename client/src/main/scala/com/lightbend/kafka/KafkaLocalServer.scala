package com.lightbend.kafka

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties
import java.util.concurrent.atomic.AtomicReference

import org.apache.curator.test.TestingServer
import org.slf4j.LoggerFactory
import javax.management.InstanceNotFoundException

import kafka.server.{KafkaConfig, KafkaServerStartable}

import scala.collection.JavaConverters._
import java.util.Comparator

import kafka.admin.{AdminUtils, RackAwareMode}
import kafka.utils.ZkUtils

class KafkaLocalServer private (kafkaProperties: Properties, zooKeeperServer: KafkaLocalServer.ZooKeeperLocalServer) {

  private val kafkaServerRef = new AtomicReference[KafkaServerStartable](null)
  private var zkUtils : ZkUtils = null

  import KafkaLocalServer._


  def start(): Unit = {
    if (kafkaServerRef.get == null) {
      // There is a possible race condition here. However, instead of attempting to avoid it 
      // by using a lock, we are working with it and do the necessary clean up if indeed we 
      // end up creating two Kafka server instances.
      val newKafkaServer = KafkaServerStartable.fromProps(kafkaProperties)
      if (kafkaServerRef.compareAndSet(null, newKafkaServer)) {
        zooKeeperServer.start()
        val kafkaServer = kafkaServerRef.get()
        kafkaServer.startup()
        zkUtils = ZkUtils.apply(s"localhost:${zooKeeperServer.getPort()}", DEFAULT_ZK_SESSION_TIMEOUT_MS, DEFAULT_ZK_CONNECTION_TIMEOUT_MS, false)
      } else newKafkaServer.shutdown()
    }
    // else it's already running
  }

  def stop(): Unit = {
    val kafkaServer = kafkaServerRef.getAndSet(null)
    if (kafkaServer != null) {
      try kafkaServer.shutdown()
      catch {
        case _: Throwable => ()
      }
      try zooKeeperServer.stop()
      catch {
        case _: InstanceNotFoundException => () // swallow, see https://github.com/Netflix/curator/issues/121 for why it's ok to do so
      }
    }
    // else it's already stopped
  }

  /**
    * Create a Kafka topic with 1 partition and a replication factor of 1.
    *
    * @param topic The name of the topic.
    */
  def createTopic(topic: String): Unit = {
    createTopic(topic, 1, 1, new Properties)
  }

  /**
    * Create a Kafka topic with the given parameters.
    *
    * @param topic       The name of the topic.
    * @param partitions  The number of partitions for this topic.
    * @param replication The replication factor for (the partitions of) this topic.
    */
  def createTopic(topic: String, partitions: Int, replication: Int): Unit = {
    createTopic(topic, partitions, replication, new Properties)
  }

  /**
    * Create a Kafka topic with the given parameters.
    *
    * @param topic       The name of the topic.
    * @param partitions  The number of partitions for this topic.
    * @param replication The replication factor for (partitions of) this topic.
    * @param topicConfig Additional topic-level configuration settings.
    */
  def createTopic(topic: String, partitions: Int, replication: Int, topicConfig: Properties): Unit = {
    AdminUtils.createTopic(zkUtils, topic, partitions, replication, topicConfig, RackAwareMode.Enforced)
  }
}

object KafkaLocalServer {
  final val DefaultPort = 9092
  final val DefaultPropertiesFile = "/kafka-server.properties"
  final val DefaultResetOnStart = true
  private val DEFAULT_ZK_CONNECT = "127.0.0.1:2181"
  private val DEFAULT_ZK_SESSION_TIMEOUT_MS = 10 * 1000
  private val DEFAULT_ZK_CONNECTION_TIMEOUT_MS = 8 * 1000


  private final val KafkaDataFolderName = "kafka_data"

  private val Log = LoggerFactory.getLogger(classOf[KafkaLocalServer])

  private lazy val tempDir = System.getProperty("java.io.tmpdir")

  def apply(cleanOnStart: Boolean): KafkaLocalServer = this(DefaultPort, ZooKeeperLocalServer.DefaultPort, DefaultPropertiesFile, Some(tempDir), cleanOnStart)

  def apply(kafkaPort: Int, zookeeperServerPort: Int, kafkaPropertiesFile: String, targetDir: Option[String], cleanOnStart: Boolean): KafkaLocalServer = {
    val kafkaDataDir = dataDirectory(targetDir, KafkaDataFolderName)
    Log.info(s"Kafka data directory is $kafkaDataDir.")

    val kafkaProperties = createKafkaProperties(kafkaPropertiesFile, kafkaPort, zookeeperServerPort, kafkaDataDir)

    if (cleanOnStart) deleteDirectory(kafkaDataDir)

    new KafkaLocalServer(kafkaProperties, new ZooKeeperLocalServer(zookeeperServerPort, cleanOnStart, targetDir))
  }

  /**
    * Creates a Properties instance for Kafka customized with values passed in argument.
    */
  private def createKafkaProperties(kafkaPropertiesFile: String, kafkaPort: Int, zookeeperServerPort: Int, dataDir: File): Properties = {
    val kafkaProperties = new Properties
    kafkaProperties.put(KafkaConfig.ListenersProp, s"PLAINTEXT://:$kafkaPort")
    kafkaProperties.put(KafkaConfig.ZkConnectProp, s"localhost:$zookeeperServerPort")
    kafkaProperties.put(KafkaConfig.BrokerIdProp, "0")
    kafkaProperties.put(KafkaConfig.HostNameProp, "127.0.0.1")
    kafkaProperties.put(KafkaConfig.NumPartitionsProp, "1")
    kafkaProperties.put(KafkaConfig.AutoCreateTopicsEnableProp, "true")
    kafkaProperties.put(KafkaConfig.MessageMaxBytesProp, "1000000")
    kafkaProperties.put(KafkaConfig.ControlledShutdownEnableProp, "true")
    kafkaProperties.setProperty(KafkaConfig.LogDirProp, dataDir.getAbsolutePath)
    kafkaProperties
  }

  private def deleteDirectory(directory: File): Unit = {
    if (directory.exists()) try {
      val rootPath = Paths.get(directory.getAbsolutePath)

      val files = Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder()).iterator().asScala
      files.foreach(Files.delete)
      Log.debug(s"Deleted ${directory.getAbsolutePath}.")
    } catch {
      case e: Exception => Log.warn(s"Failed to delete directory ${directory.getAbsolutePath}.", e)
    }
  }

  /**
    * If the passed `baseDirPath` points to an existing directory for which the application has write access,
    * return a File instance that points to `baseDirPath/directoryName`. Otherwise, return a File instance that
    * points to `tempDir/directoryName` where `tempDir` is the system temporary folder returned by the system
    * property "java.io.tmpdir".
    *
    * @param baseDirPath The path to the base directory.
    * @param directoryName The name to use for the child folder in the base directory.
    * @throws IllegalArgumentException If the passed `directoryName` is not a valid directory name.
    * @return A file directory that points to either `baseDirPath/directoryName` or `tempDir/directoryName`.
    */
  private def dataDirectory(baseDirPath: Option[String], directoryName: String): File = {
    lazy val tempDirMessage = s"Will attempt to create folder $directoryName in the system temporary directory: $tempDir"

    val maybeBaseDir = baseDirPath.map(new File(_)).filter(f => f.exists())

    val baseDir = {
      maybeBaseDir match {
        case None =>
          Log.warn(s"Directory $baseDirPath doesn't exist. $tempDirMessage.")
          new File(tempDir)
        case Some(directory) =>
          if (!directory.isDirectory()) {
            Log.warn(s"$baseDirPath is not a directory. $tempDirMessage.")
            new File(tempDir)
          } else if (!directory.canWrite()) {
            Log.warn(s"The application does not have write access to directory $baseDirPath. $tempDirMessage.")
            new File(tempDir)
          } else directory
      }
    }

    val dataDirectory = new File(baseDir, directoryName)
    if (dataDirectory.exists() && !dataDirectory.isDirectory())
      throw new IllegalArgumentException(s"Cannot use $directoryName as a directory name because a file with that name already exists in $dataDirectory.")

    dataDirectory
  }

  private class ZooKeeperLocalServer(port: Int, cleanOnStart: Boolean, targetDir: Option[String]) {
    private val zooKeeperServerRef = new AtomicReference[TestingServer](null)

    def start(): Unit = {
      val zookeeperDataDir = dataDirectory(targetDir, ZooKeeperLocalServer.ZookeeperDataFolderName)
      if (zooKeeperServerRef.compareAndSet(null, new TestingServer(port, zookeeperDataDir, /*start=*/ false))) {
        Log.info(s"Zookeeper data directory is $zookeeperDataDir.")

        if (cleanOnStart) deleteDirectory(zookeeperDataDir)

        val zooKeeperServer = zooKeeperServerRef.get
        zooKeeperServer.start() // blocking operation
      }
      // else it's already running
    }

    def stop(): Unit = {
      val zooKeeperServer = zooKeeperServerRef.getAndSet(null)
      if (zooKeeperServer != null)
        try zooKeeperServer.stop()
        catch {
          case _: IOException => () // nothing to do if an exception is thrown while shutting down
        }
      // else it's already stopped
    }
    def getPort() : Int = port
  }

  object ZooKeeperLocalServer {
    final val DefaultPort = 2181
    private final val ZookeeperDataFolderName = "zookeeper_data"
  }
}