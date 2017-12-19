# Model Serving with Akka Streams and Kafka Streams - A Tutorial

_Boris Lublinsky and Dean Wampler, Lightbend_

* Software Architecture Conference New York, Monday, February 26, 2018
* Strata Data Conference San Jose, Tuesday, March 6, 2018

This tutorial provides an introduction to streaming data applications using Kafka with Akka Streams and Kafka Streams. Hence, the tutorial helps you compare and contrast these streaming libraries for your own use.

We describe first describe how to build and run the applications. Then we will discuss their designs.

# Prerequisites

Both implementations rely on Kafka version 1.0 and leverage embedded Kafka servers (see the `client` project).

They use 2 queues:

* `models_data` - queue used for sending data
* `models_models` - queue used for sending models

The model and data "provider" applications we'll use will create an embedded Kafka server and required queues, 
as a result it has to be started before running any of the implementation.

# Building the Code

SBT is used to build the code. We recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/) for managing and building the code. The project is organized as several modules:

* `data` - some data files for running the applications
* `images` - diagrams used for this document
* `akkaserver` - Akka Streams implementation of model serving (using a custom stage)
* `akkaserverpersistent` - Akka Streams implementation of model serving (using a router and actors)
* `client` - Data and model loader used to run either the Akka Streams or Kafka Streams application. This process has to be started first to ensure that the Kafka embedded server is running.
* `configuration` - Shared configurations and InfluxDB support (see prerequisites). DeanW - you don't mention InfluxDB in the prereqs.
* `model` - Implementation of both Tensorflow and PMML models.
* `protobufs` - Shared models in protobuf format.
* `naiveserver` -  Kafka Streams implementation of model serving using in-memory storage.
* `serverstandardstore` -  Kafka Streams implementation of model serving using a standard data store.
* `server` -  Kafka Streams implementation of model serving using a custom data store.
* `scalakafkastreamingwrapper` -  Scala implementation of Kafka streams DSL. DeanW - is this Debasish's?

The build is done via SBT. IntelliJ users should just import the project and use IntelliJ commands

    cd KafkaStreamsModelServer
    sbt compile

# Deploy and Run

This project contains many executables:

* `akkaserver` - Custom stage-based Akka Streams implementation of model serving (both Scala and Java implementations are provided)
* `akkaserverpersistent` - Custom actor-based Akka Streams implementation of model serving (both Scala and Java)
* `naivekafkaserver` - Kafka Streams implementation of model serving using an in-memory store (both Scala and Java)
* `standardstorekafkaserver`- Kafka Streams implementation of model serving using a key/value store (both Scala and Java)
* `customstorekafkaserver` - Kafka Streams implementation of model serving using a custom store (both Scala and Java)
* `dataprovider` - Data and model publisher. This executable contains an embedded Kafka server and has to be started
dataprovider application has always to be start first to ensure that embedded Kafka is running and the necessary topics are created.

## Running applications

Running applications can be done either using SBT or IntelliJ
The application that should always be running first is DataProvider - `com.lightbend.kafka.client.DataProvider`
Once this application is running, the following can be started (one at a time)
* DataReader - `com.lightbend.kafka.client.DataReader` - verifies that messages are published correctly.
* Akka Server - `com.lightbend.modelServer.modelServer.AkkaModelServer` for Scala version and `com.lightbend.java.modelserver.modelserver.AkkaModelServer` for a java version.
Both implementations provide exactly the same functionality. Once either one of them is running, you can go to 
`localhost:5500/state` to obtain the current state of execution
* Akka Server Persistent - `com.lightbend.modelserver.actor.modelserver.AkkaModelServer` for Scala version and `com.lightbend.java.modelserver.actor.modelServer.AkkaModelServer` for a java version.
Both implementations provide exactly the same functionality. Once either one of them is running, you can get more information about current execution
by going to `localhost:5500/models` to obtain information about currently used models 
and to `localhost:5500/state/"model"` to obtain the current state of execution for a given model.
* Naive Kafka Streams (naive server) - `com.lightbend.naive.modelserver.scala.AkkaModelServer` for Scala version and `com.lightbend.naive.modelserver.AkkaModelServer` for a java version.
Both implementations provide exactly the same functionality. Once either one of them is running, you can get more information about current execution
by going to `localhost:8888/state/value` to obtain the current state of execution for a given model.
* Kafka Streams with Standard Store (server standard store). This project has 2 Scala implementations
`com.lightbend.standard.modelserver.scala.AkkaModelServer` and `com.lightbend.standard.modelserver.scala.ModelServerFluent`. These two implementations are here
to demonstrate the difference between using existing Java APIs from Scala (1) and usage of the fluent scala APIs (2)
Java version of the code is at com.lightbend.standard.modelserver.AkkaModelServer.
All three implementations provide exactly the same functionality. Once either one of them is running, you can get more information about current execution
by going to `localhost:8888/state/instance` to get the list of currently deployed instances and 
to `localhost:8888/state/instance` to obtain the current state of execution for a given model.
* Kafka Streams with Custom Store (server). This project has 2 Scala implementations
`com.lightbend.custom.scala.AkkaModelServer` and `com.lightbend.custom.scala.ModelServerFluent`. These two implementations are here
to demonstrate the difference between using existing Java APIs from Scala (1) and usage of the fluent scala APIs (2)
Java version of the code is at `com.lightbend.custom.modelserver.AkkaModelServer`.
All three implementations provide exactly the same functionality. Once either one of them is running, you can get more information about current execution
by going to `localhost:8888/state/instance` to get the list of currently deployed instances and 
to `localhost:8888/state/instance` to obtain the current state of execution for a given model.

# Overall Architecture

A high level view of the overall model serving architecture is as follows:

![Overall architecture of model serving](images/OverallModelServing.png)

It is similar to this [dynamically controlled stream](https://data-artisans.com/blog/bettercloud-dynamic-alerting-apache-flink), as described for [Apache Flink](https://flink.apache.org).

This architecture assumes there are two data streams - one containing data that needs to be scored, and one containing the model updates. The streaming engine contains the current model used for the actual scoring in memory. The results of scoring can be either delivered to downstream or used by the streaming engine internally as a new stream, i.e., as input for additional calculations.

If there is no model currently defined, the input data is dropped. When the new model is received, it is instantiated in memory. Once when instantiation is complete, scoring is switched to the new model. The model stream can either contain the binary blob of the data itself or the reference to the model data stored externally (pass by reference) in a database or a file system, like HDFS or S3.

This approach effectively implements model scoring as a new type of functional transformation, which can be used by any other stream functional transformations.

Although the overall architecture shown above is represented as a single model, a single streaming engine could score with multiple models simultaneously.

# Akka Streams

There are two implementations based on Akka Streams (both Scala and Java):

* An implementation based on the usage of a custom stage, which is a fully type-safe way to encapsulate the required functionality.
* An implementation based on Akka actors and the [router pattern](http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/).

The custom stage implementation uses the stream processor functionality from the overall architecture diagram.
With this component in place, the overall implementation looks as follows:

![Akka streams custom stage model serving](images/AkkaStreamsJoin.png)

For the implementation based on the actors and the router pattern, the stream processor functionality is implemented by an individual model-serving actor. A model-serving manager serves as a router passing execution for a given data type to a specific actor.

Additionally this implementation uses a simple "local file" persistence, ensuring that the application can be restarted if it crashes. The overall implementation looks as follows:

![Akka streams router model serving](images/AkkaStreamsJoinActors.png)

# Kafka Streams

The project contains three versions of the Kafka Stream implementation (both Scala and Java):

* Naive - an implementation based on internal memory and the [Processor Topology](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_processor_topology)
* Usage of a standard store - an implementation based on the Kafka Streams built-in key/value Store and the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl)
* Usage of a custom store - an implementation based on a custom store and the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl)

See [this blog post](http://mkuthan.github.io/) for a discussion of the differences between the Processor Topology and the DSL.

The Scala implementation is based on the new Scala API for Kafka streams (project scalakafkastreamwrapper). It provides an idiomatic Scala version of the fluent Java Kafka Streams API.

![Kafka streams model serving](images/KafkaStreamsJoin.png)

# Queryable State

Kafka Streams recently introduced [queryable state](https://docs.confluent.io/current/streams/developer-guide.html#id8), which is a nice approach for execution monitoring and extracting the state of a stream. This feature allows you to treat the stream processing layer as a lightweight embedded database and, more concretely, to directly query the latest state of your stream processing application, without needing to materialize that state to external databases or external storage first.

![Queriable state](images/QueryableState.png)

Both the Akka Streams and Kafka Streams implementations support queryable state. Here, we use [Akka HTTP](https://doc.akka.io/docs/akka-http/current/scala/http/) for both the Akka Streams and the Java version of the Kafka Streams queryable APIs.

# Scaling

Since both Akka and Kafka Streams implementations are in JVM implementations, they can be deployed in a cluster environment in the usual ways, as long as the source of the streams is Kafka.

The following fiture shows a Kafka Streams cluster. The Akka Streams implementation can be scaled the same way.

![scaling](images/KafkaStreamsClusters.png)

TODO - Expand


