package com.lightbend.modelServer.kafka

import org.slf4j.LoggerFactory
import org.apache.curator.test.TestingServer
import java.util.Properties

import kafka.server.KafkaConfig


object EmbeddedSingleNodeKafkaCluster {
  private val log = LoggerFactory.getLogger("EmbeddedSingleNodeKafkaCluster")
  private val DEFAULT_BROKER_PORT = 9092
  private var zookeeper = null.asInstanceOf[TestingServer]
  private var broker = null.asInstanceOf[KafkaEmbedded]

  /**
    * Creates and starts a Kafka cluster.
    */
  def start(): Unit = {
    // Ensure no duplcates
    if (broker != null)
      EmbeddedSingleNodeKafkaCluster.log.debug("Embedded Kafka cluster is already running... returning")
    else {
      val brokerConfig = new Properties
      EmbeddedSingleNodeKafkaCluster.log.debug("Initiating embedded Kafka cluster startup")
      EmbeddedSingleNodeKafkaCluster.log.debug("Starting a ZooKeeper instance")
      try
        zookeeper = new TestingServer(2181)
      catch {
        case e: Exception =>
          // TODO Auto-generated catch block
          e.printStackTrace()
      }
      EmbeddedSingleNodeKafkaCluster.log.debug(s"ZooKeeper instance is running at $zKConnectString")
      brokerConfig.put(KafkaConfig.ZkConnectProp, zKConnectString)
      brokerConfig.put(KafkaConfig.PortProp, DEFAULT_BROKER_PORT.toString)
      EmbeddedSingleNodeKafkaCluster.log.debug(s"Starting a Kafka instance on port ${brokerConfig.getProperty(KafkaConfig.PortProp)} ...")
      broker = new KafkaEmbedded(brokerConfig)
      broker.start()
      EmbeddedSingleNodeKafkaCluster.log.debug(s"Kafka instance is running at ${broker.brokerList}, connected to ZooKeeper at ${broker.zookeeperConnect}")
    }
  }

  /**
    * Stop the Kafka cluster.
    */

  def stop(): Unit = {
    try {
      broker.stop()
      zookeeper.stop()
    } catch {
      case t: Throwable =>
    }
  }

  /**
    * The ZooKeeper connection string aka `zookeeper.connect` in `hostnameOrIp:port` format.
    * Example: `127.0.0.1:2181`.
    *
    * You can use this to e.g. tell Kafka brokers how to connect to this instance.
    */
  def zKConnectString: String = zookeeper.getConnectString

  /**
    * This cluster's `bootstrap.servers` value.  Example: `127.0.0.1:9092`.
    *
    * You can use this to tell Kafka producers how to connect to this cluster.
    */
  def bootstrapServers: String = broker.brokerList

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
    broker.createTopic(topic, partitions, replication, topicConfig)
  }
}