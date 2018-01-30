# Model Serving with Akka Streams and Kafka Streams - A Tutorial

[Boris Lublinsky](mailto:boris.lublinsky@lightbend.com) and [Dean Wampler](mailto:dean.wampler@lightbend.com), [Lightbend](https://lightbend.com/fast-data-platform)

* [Software Architecture Conference New York, Monday, February 26, 2018](https://conferences.oreilly.com/software-architecture/sa-ny/public/schedule/detail/63999)
* [Strata Data Conference San Jose, Tuesday, March 6, 2018](https://conferences.oreilly.com/strata/strata-ca/public/schedule/detail/63983)
* [Strata Data Conference London, Tuesday, May 22, 2018](https://conferences.oreilly.com/strata/strata-eu/public/schedule/detail/65420)

This tutorial provides an introduction to streaming data microservices using Kafka with Akka Streams and Kafka Streams. Hence, the tutorial helps you compare and contrast these streaming libraries for your own use.

We will first describe how to build and run the applications. Then we will discuss their designs.

## Tutorial Setup

TODO

[SBT](https://www.scala-sbt.org/) is used to build the code. We recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/) for managing and building the code, which can drive SBT. However, this is isn't required; any favorite IDE or editor environment will do.

If you wish to use SBT in a terminal (e.g., in conjunction with a regular text editor), follow the SBT installation instructions [here](https://www.scala-sbt.org/download.html).

If you use IntelliJ IDEA (the Community Edition is sufficient) or another IDE environment, also install available Scala and SBT plugins for these IDEs.

## Prerequisites

Implementation rely on Kafka version 1.0 and leverage embedded Kafka servers (see the `client` project). Using embedded Kafka servers, rather than standalone Kafka services, simplifies the setup and execution of the tutorial.

The implementations use two queues:

* `models_data` - queue used for sending data
* `models_models` - queue used for sending machine learning model updates, used for scoring the data

The model and data "provider" application we use will create an embedded Kafka server and the required queues, as a result it has to be started before running any of the implementations.

## Project Overviewhttps://github.com/lightbend/fdp-kafka-akka-streams-tutorial.git

The project is organized as several subdirectories, some of which are setup as nested SBT projects, while others provide supporting functions, like the `data` directory:

* `data` - some data files for running the applications
* `images` - diagrams used for this document
* `akkaStreamsCustomStage` - (project) Akka Streams implementation of model serving using a custom stage
* `akkaActorsPersistent` - (project) Akka Actors and Akka Streams implementation of model serving that also provides state persistence to disk
* `client` - (project) Data and model loader used to run either the Akka Streams or Kafka Streams application. This process has to be started first to ensure that the Kafka embedded server is running
* `configuration` - Shared configurations
* `model` - Implementation of internal, Tensorflow and PMML models
* `protobufs` - Protobuf representation of both models aand data
* `kafkaStreamsModelServerInMemoryStore` -  Kafka Streams implementation of model serving using the [Processor Topology](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_processor_topology) APIs and in-memory ("naive") storage
* `kafkaStreamsModelServerKVStore` -  Kafka Streams implementation of model serving using a key-value store provided by Kafka Streams and the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl)
* `kafkaStreamsModelServerCustomStore` -  Kafka Streams implementation of model serving using a custom store and the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl)
* `scalakafkastreamingwrapper` -  Scala implementation of Kafka streams DSL. TODO - is this Debasish's new library? If so, can we switch to that?

You can see the list of SBT projects using the `sbt projects` command.  Note that the top level project, in the root directory, is called `akkaKafkaTutorial`.

We'll explore these in more detail after discussing how to build the code.

## Building the Code

Because many of these subdirectories contain both Scala and Java implementations, but often use the same class names, we use the package naming convention `com.lightbend.scala` for (almost) all Scala code and `com.lightbend.java` for (almost) all Java code. An exception is the code generated in the `protobufs` directory; no `scala` or `java` names are used. If you're new to Scala, this naming scheme is our convention; it's not commonly used in projects.

The build is done via SBT, using the `sbt` shell command. To build in a terminal outside your IDE/Editor environment, use this command:

    sbt compile

You can also use IntelliJ to manage the project and run the SBT build:

1. Use the Import Project feature to import the top-level directory of this repo as a project.
2. Select SBT as the project type
3. In the settings dialog, check _Use sbt shell for build and import_.
4. After the project loads, open the SBT console, and click the green arrow to start SBT
5. After it presents the prompt `sbt:akkaKafkaTutorial>`, type `compile`.

> **TIP:** You can also build subprojects one at a time. Use `sbt projects` to list the projects, then `project <name>` to switch to that subproject. Now use commands like `clean`, `compile`, etc.

For other IDEs and editors, consult their documentation on using SBT and Scala. If your editor doesn't offer Scala and SBT support, just load the project as a Java project and use the editor to browse and edit the files. Use a terminal to run SBT.

## Running the Executables

Most of the SBT projects listed above have service executables. The top level project in the root directory is called `akkaKafkaTutorial`.

Because nested SBT projects are used, you can't just type `run <main_class>` (e.g., `com.lightbend.scala.kafka.client.DataProvider`). Instead, use the following convention that specifies the SBT project, the `client` in this example, then select the executable to run from the list presented:

```
sbt:akkaKafkaTutorial> client/run

Multiple main classes detected, select one to run:

 [1] com.lightbend.scala.kafka.client.DataProvider
 [2] com.lightbend.scala.kafka.client.DataReader

Enter number: 1
...
```

To run a specific executable, for example `DataProvider`, use `runMain`:

```
sbt:akkaKafkaTutorial> client/runMain com.lightbend.scala.kafka.client.DataProvider
...
```

If you use the `sbt` command in a terminal, you'll need one terminal for each service executed below.

Alternatively you can run execution directly from IntelliJ. 
To do this, just click on the file containing the scala/java main and on the pop-up click run (or debug).

## Project Details

Let's first discuss the overall architecture and then discuss the various examples in more depth, including what executables to run and when.

### Overall Architecture

A high level view of the overall model serving architecture is as follows:

![Overall architecture of model serving](images/OverallModelServing.png)

It is similar to this [dynamically controlled stream](https://data-artisans.com/blog/bettercloud-dynamic-alerting-apache-flink), as described for [Apache Flink](https://flink.apache.org).

This architecture assumes there are two data streams - one containing data that needs to be scored, and one containing the model updates. The streaming engine contains the current model used for the actual scoring in memory. The results of scoring can be either delivered downstream or used by the streaming engine internally as a new stream, i.e., as input for additional calculations.

If there is no model currently defined, the input data is dropped. When the new model is received, it is instantiated in memory. Once instantiation is complete, scoring is switched to the new model. 
The model stream can either contain the binary blob of the data itself or the reference to the model data stored externally (pass by reference) in a database or a file system, like HDFS or S3. Only pass by value is implemented in the code.

This approach effectively implements model scoring as a new type of functional transformation, which can be used by any other stream transformations.

Although the overall architecture shown above is represented as a single model, a single streaming engine could score with multiple models simultaneously.

### Queryable State

Kafka Streams recently introduced [queryable state](https://docs.confluent.io/current/streams/developer-guide.html#id8), which is a nice approach for execution monitoring and extracting the state of a stream. This feature allows you to treat the stream processing layer as a lightweight embedded database and, more concretely, to directly query the latest state of your stream processing application, without needing to materialize that state to external databases or external storage first.

![Queriable state](images/QueryableState.png)

Both the Akka Streams and Kafka Streams implementations support queryable state. Here, we use [Akka HTTP](https://doc.akka.io/docs/akka-http/current/scala/http/) for Web based access to both the Akka Streams and Kafka Streams queryable APIs.


### Client

Now we'll discuss the example implementations, starting with the `client`, since it has to be executed first (to set up Kafka). Then we'll list the rest of the projects alphabetically. We'll focus on the classes (Scala and Java types) that provide executables, but not the other classes in these projects. We'll discuss those verbally during the tutorial.

The service that starts the embedded Kafka server, creates the topics, and publishes data to them.

| | | |
| :- | :- | :- |
| SBT Project | `client` | |
| Directory   | `client` | |
| Data Provider | [com.lightbend.scala.kafka.client.DataProvider](client/src/main/scala/com/lightbend/scala/kafka/client/DataProvider.scala) | (Scala only) Provider service |
| Data Reader | [com.lightbend.scala.kafka.client.DataReader](client/src/main/scala/com/lightbend/scala/kafka/client/DataReader.scala) | Verifies that messages are published correctly |

The `DataProvider` is a data and model publisher. This executable contains an embedded Kafka server and has to be started first to ensure that the embedded Kafka server is running and the necessary topics are created.

Start the `DataProvider` as using SBT as shown above:

```
sbt:akkaKafkaTutorial> client/run

Multiple main classes detected, select one to run:

 [1] com.lightbend.scala.kafka.client.DataProvider
 [2] com.lightbend.scala.kafka.client.DataReader

Enter number: 1
...
```

Or use this command to run `DataProvider` without the prompt:

```
sbt:akkaKafkaTutorial> client/runMain com.lightbend.scala.kafka.client.DataProvider
...
```

Alternatively you can run it from within IntelliJ by clicking at `com.lightbend.scala.kafka.client.DataProvider` and pick `Run DataProvider`

You can run the `DataReader` when desired the same way.

### Akka Streams Custom Stage Model Server

There are two versions of model serving based on Akka Streams (with both Scala and Java implementations):

* An implementation based on the usage of a custom "stage", which is a fully type-safe way to encapsulate the required functionality (`akkaStreamsCustomStage` - this one)
* An implementation based on Akka actors and the [router pattern](http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/) (`akkaActorsPersistent` - the next one)

The custom stage implementation uses the stream processor functionality from the overall architecture diagram shown above. The overall implementation looks as follows:

![Akka streams custom stage model serving](images/AkkaStreamsJoin.png)

| | |
| :- | :- |
| SBT Project | `akkaStreamsCustomStage` |
| Directory   | `akkaStreamsCustomStage` |
| Scala Version | [com.lightbend.scala.modelServer.modelServer.AkkaModelServer](akkaStreamsCustomStage/src/main/scala/com/lightbend/scala/modelServer/modelServer/AkkaModelServer.scala) |
| Java Version  | [com.lightbend.java.modelserver.modelserver.AkkaModelServer](akkaStreamsCustomStage/src/main/java/com/lightbend/java/modelserver/modelserver/AkkaModelServer.java) |

Start one of the applications by running `sbt akkaStreamsCustomStage/run` in a terminal window, then select the executable to run. Or use `runMain`, e.g., for the Scala version:

```
sbt:akkaKafkaTutorial> akkaStreamsCustomStage/runMain com.lightbend.scala.modelServer.modelServer.AkkaModelServer

...
```
Alternatively you can run it from within IntelliJ by clicking at `com.lightbend.scala.modelServer.modelServer.AkkaModelServer` (Scala) 
or `com.lightbend.java.modelserver.modelserver.AkkaModelServer` (java)
and pick `Run AkkaModelServer`

Once either server is running, 
go to http://localhost:5500/state to obtain the current state of execution.

> **Note:** Only run _one_ of the Akka model servers or the Kafka model servers at the same time, since they share the same ports to serve information.

### Akka Streams Server with Akka Persistence

This is the implementation based on Akka actors and the [router pattern](http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/) (`akkaActorsPersistent`)

The stream processor functionality is implemented by an individual model-serving actor. A model-serving manager serves as a router, passing execution for a given data type to a specific actor.

Additionally this implementation uses a simple "local file" persistence, using [Akka Persistence](https://doc.akka.io/docs/akka/2.5.4/scala/persistence.html), which insures that the application can be restarted if it crashes. Hence, this feature helps support the resiliency required for production systems.

The overall implementation looks as follows:

![Akka streams router model serving](images/AkkaStreamsJoinActors.png)

| | |
| :- | :- |
| SBT Project | `akkaActorsPersistent` |
| Directory   | `akkaActorsPersistent` |
| Scala Version | [com.lightbend.scala.modelserver.actor.modelserver.AkkaModelServer](akkaActorsPersistent/src/main/scala/com/lightbend/scala/modelserver/actor/modelserver/AkkaModelServer.scala) |
| Java Version  | [com.lightbend.java.modelserver.actor.modelserver.AkkaModelServer](akkaActorsPersistent/src/main/java/com/lightbend/java/modelserver/actor/modelserver/AkkaModelServer.java) |

Start one of the applications by running `sbt akkaActorsPersistent/run` in a terminal window, then select the executable to run. Or use `runMain`, e.g., for the Scala version:

```
sbt:akkaKafkaTutorial> akkaActorsPersistent/runMain com.lightbend.scala.modelServer.actor.modelServer.AkkaModelServer
...
```
Alternatively you can run it from within IntelliJ by clicking 
at `com.lightbend.scala.modelserver.actor.modelserver.AkkaModelServer` (Scala) 
or `com.lightbend.java.modelserver.actor.modelServer.AkkaModelServer` (java)
and pick `Run AkkaModelServer`

Once running, visit these links:

* http://localhost:5500/models for information about the currently-used models
* http://localhost:5500/state/"a_model" for the current state of execution for `a_model`, as listed in the previous link.

## Kafka Streams Model Server

Three versions of model serving are provided using Kafka Streams, with implementations in both Scala and Java:

* "Naive" - an implementation that "persists" state in internal memory (which is "naive") and uses the [Processor Topology](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_processor_topology) API (`kafkaStreamsModelServerInMemoryStore` - two projects below)
* Key-value store - an implementation based on the Kafka Streams built-in key-value Store and the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl) (`kafkaStreamsModelServerKVStore` - the next project)
* A custom store - an implementation based on a custom store (files) and the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl) (`kafkaStreamsModelServerCustomStore` - this project)

See [this blog post](http://mkuthan.github.io/) for a discussion of the differences between the Kafka Streams Processor Topology and the DSL.

For the Scala implementations based on Kafka Stream DSL, in addition to leveraging Kafka Java APIs we are also showing 
implementations based on the new [Scala API for Kafka Streams](https://github.com/lightbend/kafka-streams-scala) that Lightbend recently open sourced. 
(It will be contributed to the Kafka project once the API details have settled.) It provides an idiomatic Scala version of the fluent Java Kafka Streams API. 
See also the [Kafka Streams Query API](https://github.com/lightbend/kafka-streams-query), which provides a convenient way to query stream state across a cluster. (not used in this tutorial)

The model serving implementation is illustrated here:

![Kafka streams model serving](images/KafkaStreamsJoin.png)

### Kafka Streams Model Server with an In-memory ("Naive") Store

This Kafka Streams implementation of model serving "persists" state in memory, which is "naive". Both Scala and Java implementations are provided for model serving that provide identical functionality.

| | |
| :- | :- |
| SBT Project | `kafkaStreamsModelServerInMemoryStore` |
| Directory   | `kafkaStreamsModelServerInMemoryStore` |
| Scala Version | [com.lightbend.scala.naive.modelserver.ModelServer](kafkaStreamsModelServerInMemoryStore/src/main/scala/com/lightbend/scala/naive/modelserver/ModelServer.scala) |
| Java Version | [com.lightbend.java.naive.modelserver.ModelServer](kafkaStreamsModelServerInMemoryStore/src/main/java/com/lightbend/java/naive/modelserver/ModelServer.java) |

Start one of the applications by running `sbt kafkaStreamsModelServerInMemoryStore/run` in a terminal window, then select the executable to run. Or use `runMain`, e.g., for the Scala version:

```
sbt:akkaKafkaTutorial> kafkaStreamsModelServerInMemoryStore/runMain com.lightbend.scala.naive.modelserver.ModelServer
...
```

Alternatively you can run it from within IntelliJ by clicking 
at `com.lightbend.scala.naive.modelserver.ModelServer` (Scala) 
or `com.lightbend.java.naive.modelserver.ModelServer` (java)
and pick `Run ModelServer`


Once running, visit http://localhost:8888/state/value for the current state of execution for a given model.


### Kafka Streams Model Server with a Key-Value Store

The second Kafka Streams example of model serving uses the provided key-value store in Kafka Streams. Two Scala and Java implementations are provided for model serving that provide identical functionality. An additional Scala implementation is provided that uses a "fluent" API.

| | |
| :- | :- |
| SBT Project | `kafkaStreamsModelServerKVStore` |
| Directory   | `kafkaStreamsModelServerKVStore` |
| Scala Version | [com.lightbend.scala.standard.modelserver.ModelServer](kafkaStreamsModelServerKVStore/src/main/scala/com/lightbend/scala/standard/modelserver/ModelServer.scala) |
| Java Version | [com.lightbend.java.standard.modelserver.ModelServer](kafkaStreamsModelServerKVStore/src/main/java/com/lightbend/java/standard/modelserver/ModelServer.java) |
| Scala "Fluent" Version | [com.lightbend.scala.standard.modelserver.ModelServerFluent](kafkaStreamsModelServerKVStore/src/main/scala/com/lightbend/scala/standard/modelserver/ModelServerFluent.scala) |

Start one of the applications by running `sbt kafkaStreamsModelServerKVStore/run` in a terminal window, then select the executable to run. Or use `runMain`, e.g., for the Scala version:

```
sbt:akkaKafkaTutorial> kafkaStreamsModelServerKVStore/runMain com.lightbend.scala.standard.modelserver.ModelServer
...
```

Alternatively you can run it from within IntelliJ by clicking 
at `com.lightbend.scala.standard.modelserver.scala.ModelServer` (Scala, based on Java APIs) 
or `com.lightbend.scala.standard.modelserver.scala.ModelServerFluent` (Scala, based on Scala APIs) 
or `com.lightbend.java.standard.modelserver.ModelServer` (java)
and pick `Run ModelServer` or `Run ModelServerFluent`


Once running, visit these links:

* http://localhost:8888/state/instance for the list of currently deployed instances
* http://localhost:8888/state/value for the current state of execution for a given model.


### Kafka Streams Model Server with a Custom Store

The final Kafka Streams example of model serving demonstrates implementation of a custom store in Kafka Streams. 
Two Scala and Java implementations are provided for model serving that provide identical functionality. An additional Scala implementation is provided that uses a "fluent" API.
| | |
| :- | :- |
| SBT Project | `kafkaStreamsModelServerCustomStore` |
| Directory   | `kafkaStreamsModelServerCustomStore` |
| Scala Version | [com.lightbend.scala.custom.modelserver.ModelServer](kafkaStreamsModelServerCustomStore/src/main/scala/com/lightbend/scala/custom/modelserver/ModelServer.scala) |
| Java Version | [com.lightbend.java.custom.modelserver.ModelServer](kafkaStreamsModelServerCustomStore/src/main/java/com/lightbend/java/custom/modelserver/ModelServer.java) |
| Scala "Fluent" Version | [com.lightbend.scala.custom.modelserver.ModelServerFluent](kafkaStreamsModelServerCustomStore/src/main/scala/com/lightbend/scala/custom/modelserver/ModelServerFluent.scala) |

Start one of the applications by running `sbt kafkaStreamsModelServerCustomStore/run` in a terminal window, then select the executable to run. Or use `runMain`, e.g., for the Scala version:

```
sbt:akkaKafkaTutorial> kafkaStreamsModelServerCustomStore/runMain com.lightbend.scala.custom.modelserver.ModelServer
...
```
Alternatively you can run it from within IntelliJ by clicking 
at `com.lightbend.scala.custom.modelserver.ModelServer` (Scala, based on Java APIs) 
or `com.lightbend.scala.custom.modelserver.ModelServerFluent` (Scala, based on Scala APIs) 
or `com.lightbend.java.standard.modelserver.ModelServer` (java)
and pick `Run ModelServer` or `Run ModelServerFluent`


Once running, visit these links:

* http://localhost:8888/state/instance for the list of currently deployed instances
* http://localhost:8888/state/value for the current state of execution for a given model.



## Production Considerations

The examples illustrate how to implement a model-serving microservice using Akka Streams and Kafka Streams in various ways. A few illustrate other production considerations, such as persisting state so that restarts can pick up where processing left off.

Here we consider a few other production considerations.

### Scaling

Since both Akka and Kafka Streams implementations are in JVM implementations, they can be deployed in a cluster environment in the usual ways, as long as the source of the streams is Kafka.

The following diagram shows a Kafka Streams cluster with several server instances, Kafka topic partitions, etc. An Akka Streams implementation can be scaled a similar way.

![scaling](images/KafkaStreamsClusters.png)

TODO - Expand


