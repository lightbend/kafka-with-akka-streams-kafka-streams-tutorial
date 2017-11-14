# Model serving Akka Streams and Kafka Streams

# Overall Architecture

A high level view of the overall model serving architecture (
similar to [dynamically controlled stream](https://data-artisans.com/blog/bettercloud-dynamic-alerting-apache-flink)) 
![Overall architecture of model serving](images/overallModelServing.png)


This architecture assumes two data streams - one containing data that needs to be scored, and one containing the model updates. The streaming engine contains the current model used for the actual scoring in memory. The results of scoring can be either delivered to the customer or used by the streaming engine internally as a new stream - input for additional calculations. If there is no model currently defined, the input data is dropped. When the new model is received, it is instantiated in memory, and when instantiation is complete, scoring is switched to a new model. The model stream can either contain the binary blob of the data itself or the reference to the model data stored externally (pass by reference) in a database or a filesystem, like HDFS or S3. 
Such approach effectively using model scoring as a new type of functional transformation, that can be used by any other stream functional transformations.
Although the overall architecture above is showing a single model, a single streaming engine could score multiple models simultaneously. 

# Akka Streams

Akka implementation is based on the usage of a custom stage, which is a fully type-safe way to encapsulate required functionality. 
The stage implements stream processor functionality from the overall architecture diagram. 
With such component in place, the overall implementation is going to look as follows:


![Akka streams model serving](images/Akkajoin.png)


# Kafka Streams

The project contains three versions of Kafka Stream implementation:
* Naive - implementation based on internal memory and [Process Topology](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_processor_topology)
* Usage of standard store - implementation based on Kafka Streams key/value Store and Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl)
* Usage of a custom store - implementation based on a Custom Store and Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl)

Reffer to [discussion](http://mkuthan.github.io/) on differences between Process Topology and DSL

Kafka Streams implementation leverages custom store containing current execution state.
With this store in place, implementation of the model serving using Kafka 
Streams becomes very simple, it’s basically two independent streams coordinated via a shared store. 


![Kafka streams model serving](images/kafkastreamsJoin.png)

# Queryable state

Kafka Streams  recently [introduced](https://docs.confluent.io/current/streams/developer-guide.html#id8) queryable state, which is 
a nice approach to execution monitoring.
This feature allows to treat the stream processing layer as a 
lightweight embedded database and, more concretely, to directly query the latest state of your stream processing application, without needing to materialize that state to external databases or external storage first.


![Queriable state](images/queryablestate.png)

Both Akka Streams and Kafka streams implementation support queryable state

# Scaling

Both Akka and Kafka Streams implementations are in JVM implementations.
If the source of streams is Kafka, they both can be deployed as a cluster.
Fig below shows Kafka Streams cluster. Akka Streams implementation can be scaled the same way

![scaling](images/Kafkastreamsclusters.png)


# Prerequisites

Overall implement relies on Kafka (current version is 1.0) and requires kafka to be installed.
It uses 2 queues:
* `models_data` - queue used for sending data
* `models_models` - queue used for sending models
Model provider and data provider applications check if their corresponding queues exist. Run them 
first if not sure whether queues exist

It also relies on InfluxDB/Grafana for visualization. Both need to be installed before running applications

For InfluxDB database `serving` with retentionPolicy `default` is used. Application checks on startup whether the database exists and create it, if necessary
Application also ensures that Grafana data source and dashboard definitions exist and create them, in necessary


# Build the code

We recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/) for managing and building the code. The project is organized as several modules:

* `data` - some data files for running the applications
* `images` - diagrams used for this document
* `akkaserver` - Akka Streams implementation of model serving
* `client` - Data and model loader used to run either Akka or Kafka streams application
* `configuration` - Shared configurations anf InfluxDB support (see prerequisites)
* `model` - Implementation of both Tensorflow anf PMML models.
* `protobufs` - Shared models in protobuf format.
* `naiveserver` -  Kafka Streams implementation of model serving using in memory storage.
* `serverstandardstore` -  Kafka Streams implementation of model serving using standard data store.
* `server` -  Kafka Streams implementation of model serving using custom data store.

The build is done via SBT

    cd KafkaStreamsModelServer
    sbt compile
    # For IntelliJ users, just import a project and use IntelliJ commands

# Deploy and Run

This project contains 6 executables:
* `akkaserver` - Akka Streams implementation of model serving
* `naivekafkaserver` - Kafka Streams implementation of model serving using in memory store
* `standardstorekafkaserver`- Kafka Streams implementation of model serving using key/value store
* `customstorekafkaserver` - Kafka Streams implementation of model serving using custom store
* `dataprovider` - Data and model publisher

Each application can run either locally (on user's machine) or on the server.

## Running locally

Running locally can be done either using SBT or Intellij (If you run locally, make sure to change 
kafka configuration `(broker quarum and zookeeper)`), InfluxDB configuration `(host and port)` and 
Grafana configuration `(host and port)`

Both `dataprovider` and `modelprovider` applications allow for changing of frequency of sending data, by
specifying desired frequency (in ms) as an application parameter. If the parameter is not specified
data is send once a sec and model - once every 5 mins.

Both `akkaserver` and `kafkaserver` implement queryable state. 

To query `akkaserver` state connect your browser to `host:5500/stats` to get statistics of the current execution
Currently `akkaserver` supports only statistics for a given server. If a cluster is used, each server needs to be
queried (with the same port)


To query `kafkaserver` state connect your browser to `host:8888`. This contains several URLs:
* `/state/instances` returns the list of instances containing a store Naive implementation does not support this one. 
* `/state/value` returns current state of the model serving




