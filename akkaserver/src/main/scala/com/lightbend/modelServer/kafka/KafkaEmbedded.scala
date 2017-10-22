package com.lightbend.modelServer.kafka

import com.google.common.io.Files
import kafka.admin.{ AdminUtils, RackAwareMode }
import kafka.server.{ KafkaConfig, KafkaServerStartable }
import kafka.utils.{ CoreUtils, ZkUtils, ZKStringSerializer }
import org.I0Itec.zkclient.{ ZkClient, ZkConnection }
import org.slf4j.LoggerFactory
import java.io.File
import java.util.{ Collections, Properties }

/**
 * Runs an in-memory, "embedded" instance of a Kafka broker, which listens at `127.0.0.1:9092` by
 * default.
 *
 * Requires a running ZooKeeper instance to connect to.
 */
object KafkaEmbedded {
  private val log = LoggerFactory.getLogger(classOf[KafkaEmbedded])
  private val DEFAULT_ZK_CONNECT = "127.0.0.1:2181"
  private val DEFAULT_ZK_SESSION_TIMEOUT_MS = 10 * 1000
  private val DEFAULT_ZK_CONNECTION_TIMEOUT_MS = 8 * 1000

  def createTempDir: File = Files.createTempDir

  def apply(config: Properties): KafkaEmbedded = new KafkaEmbedded(config)
}

class KafkaEmbedded(val config: Properties) {

  /**
   * Creates and starts an embedded Kafka broker.
   *
   * @param config Broker configuration settings.  Used to modify, for example, on which port the
   *               broker should listen to.  Note that you cannot change the `log.dirs` setting
   *               currently.
   */
  val logDir = KafkaEmbedded.createTempDir
  val effectiveConfig = effectiveConfigFrom(config)
  val loggingEnabled = true
  val kafkaConfig = new KafkaConfig(effectiveConfig, loggingEnabled)
  KafkaEmbedded.log.debug(s"Starting embedded Kafka broker (with log.dirs=$logDir and ZK ensemble at $zookeeperConnect) ...")
  val kafka = new KafkaServerStartable(kafkaConfig)
  val zkUtils = ZkUtils.apply(zookeeperConnect, KafkaEmbedded.DEFAULT_ZK_SESSION_TIMEOUT_MS, KafkaEmbedded.DEFAULT_ZK_CONNECTION_TIMEOUT_MS, false)
  KafkaEmbedded.log.debug(s"Startup of embedded Kafka broker at brokerList completed (with ZK ensemble at $zookeeperConnect) ...")

  private def effectiveConfigFrom(initialConfig: Properties) = {
    val effectiveConfig = new Properties
    effectiveConfig.put(KafkaConfig.BrokerIdProp, "0")
    effectiveConfig.put(KafkaConfig.HostNameProp, "127.0.0.1")
    effectiveConfig.put(KafkaConfig.PortProp, "9090")
    effectiveConfig.put(KafkaConfig.NumPartitionsProp, "1")
    effectiveConfig.put(KafkaConfig.AutoCreateTopicsEnableProp, "true")
    effectiveConfig.put(KafkaConfig.MessageMaxBytesProp, "1000000")
    effectiveConfig.put(KafkaConfig.ControlledShutdownEnableProp, "true")
    effectiveConfig.putAll(initialConfig)
    effectiveConfig.setProperty(KafkaConfig.LogDirProp, logDir.getAbsolutePath)
    effectiveConfig
  }

  /**
   * This broker's `metadata.broker.list` value.  Example: `127.0.0.1:9092`.
   *
   * You can use this to tell Kafka producers and consumers how to connect to this instance.
   */
  def brokerList: String = String.join(":", kafka.serverConfig.hostName, Integer.toString(kafka.serverConfig.port))

  /**
   * The ZooKeeper connection string aka `zookeeper.connect`.
   */
  def zookeeperConnect: String = effectiveConfig.getProperty("zookeeper.connect", KafkaEmbedded.DEFAULT_ZK_CONNECT)

  /**
   * Start the broker.
   */
  def start(): Unit = {
    KafkaEmbedded.log.debug(s"Starting embedded Kafka broker at $brokerList (with log.dirs=$logDir and ZK ensemble at $zookeeperConnect) ...")
    kafka.startup()
    KafkaEmbedded.log.debug(s"Startup of embedded Kafka broker at $brokerList completed (with ZK ensemble at $zookeeperConnect) ...")
  }

  /**
   * Stop the broker.
   */
  def stop(): Unit = {
    KafkaEmbedded.log.debug(s"Shutting down embedded Kafka broker at $brokerList (with ZK ensemble at $zookeeperConnect) ...")
    kafka.shutdown()
    kafka.awaitShutdown()
    KafkaEmbedded.log.debug(s"Removing logs.dir at $logDir ...")
    val logDirs = Collections.singletonList(logDir.getAbsolutePath)
    logDir.delete
    CoreUtils.delete(scala.collection.JavaConversions.asScalaBuffer(logDirs).seq)
    KafkaEmbedded.log.debug(s"Shutdown of embedded Kafka broker at $brokerList completed (with ZK ensemble at $zookeeperConnect) ...")
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
    KafkaEmbedded.log.debug(s"Creating topic { name: $topic, partitions: $partitions, replication: $replication, config: $topicConfig }")
    //  val zkClient = new ZkClient(zookeeperConnect, KafkaEmbedded.DEFAULT_ZK_SESSION_TIMEOUT_MS, KafkaEmbedded.DEFAULT_ZK_CONNECTION_TIMEOUT_MS, ZKStringSerializer)
    //  val zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperConnect), false)
    AdminUtils.createTopic(zkUtils, topic, partitions, replication, topicConfig, RackAwareMode.Enforced)
  }
}