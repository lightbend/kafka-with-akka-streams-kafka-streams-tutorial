# Building Kafka-based Microservices with Akka Streams and Kafka Streams - a Tutorial

[![Join the chat at https://gitter.im/kafka-with-akka-streams-kafka-streams-tutorial](https://badges.gitter.im/kafka-with-akka-streams-kafka-streams-tutorial.svg)](https://gitter.im/kafka-with-akka-streams-kafka-streams-tutorial?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

> **NOTE:** This tutorial is a work in progress. The "1.0" version will be available in time for the Software Architecture Conf tutorial, February 26.

[Boris Lublinsky](mailto:boris.lublinsky@lightbend.com) and [Dean Wampler](mailto:dean.wampler@lightbend.com), [Lightbend](https://lightbend.com/fast-data-platform)

* [Software Architecture Conference New York, Monday, February 26, 2018](https://conferences.oreilly.com/software-architecture/sa-ny/public/schedule/detail/63999)
* [Strata Data Conference San Jose, Tuesday, March 6, 2018](https://conferences.oreilly.com/strata/strata-ca/public/schedule/detail/63983)
* [Strata Data Conference London, Tuesday, May 22, 2018](https://conferences.oreilly.com/strata/strata-eu/public/schedule/detail/65420)

This tutorial provides an introduction to streaming data microservices using Kafka with Akka Streams and Kafka Streams. Hence, the tutorial helps you compare and contrast these streaming libraries for your own use.

The core "use case" implemented is a stream processing application that also ingests updated parameters for a machine learning model and then uses the model to score the data. Several implementations of this use case are provided. They not only compare Akka Streams vs. Kafka Streams, but they also show how to support a few other common production requirements, such as managing the in-memory state of the application.

First, we will describe how to build and run the applications. Then we will discuss their designs. For reference materials and more information, see the end of this README.

## Tutorial Setup

> **Note:** If you are attending this tutorial at a conference, please follow the setup steps _ahead of time_. If you encounter problems, ask for help on the project's [Gitter room](https://gitter.im/kafka-with-akka-streams-kafka-streams-tutorial).

### Install the Required Tools

The Java JDK v8 is required. If not already installed, see the instructions [here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

[SBT](https://www.scala-sbt.org/), the _de facto_ build tool for Scala is used to build the code, both the Scala and Java implementations. The SBT build files are configured to download all the required dependencies.

We recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/) for managing and building the code, which can drive SBT. The free Community Edition is sufficient. However, using IntelliJ isn't required; any favorite IDE or editor environment will do.

If you use IntelliJ IDEA or another IDE environment, also install the Scala plugin for the IDE. IntelliJ's Scala plugin includes support for SBT (ignore the SBT plugins that are available). Other IDEs might require a separate SBT plugin. Note that the tutorial uses the last release of Scala 2.11, 2.11.12 (the overall latest release of Scala is 2.12.4).

> **Note:** If you encounter class file or byte code errors when attempting to run SBT below, try removing any versions of Scala that are on your `PATH`. You can also try downloading the correct version of Scala ([tgz](https://downloads.lightbend.com/scala/2.11.12/scala-2.11.12.tgz) or [zip](https://downloads.lightbend.com/scala/2.11.12/scala-2.11.12.zip)) and use it as your Scala SDK for the project or the IDE globally.

If you use IntelliJ, the quickest way to start is to create a new project from the GitHub repository:

1. File > New > Project from Version Control > GitHub
2. Log into your GitHub account
3. Specify the URL https://github.com/lightbend/kafka-with-akka-streams-kafka-streams-tutorial
4. When the window opens, you'll see a pop-up with a link asking to load the SBT project; do that
5. Accept the defaults for SBT. Use JDK 1.8 if it's not shown as the default.
6. Do one build using the SBT command line...

Unfortunately, the IntelliJ build doesn't properly build the `protobuf` project (TBD), so do a one-time command-line build:

1. Open the terminal tool window in IntelliJ (e.g., _View > Tool Windows > Terminal_)
2. Type "sbt compile"
3. It should end with `[success] Total time: 30 s, completed Feb ...` after ~30 seconds
4. Now just use IntelliJ's _Build_ command as needed or automatically

If you don't have a GitHub account, just download the latest [release](https://github.com/lightbend/kafka-with-akka-streams-kafka-streams-tutorial/releases) and import the code as an SBT project into your IDE. In IntelliJ, use these steps:

* _Import Project_
* Select the project root directory (i.e., the same as for this README)
* Select `sbt` as the project type
* Use the default settings for `sbt`. Use JDK 1.8 if it's not shown as the default.
* Profit!!

If you wish to use SBT in a terminal (e.g., in conjunction with your text editor), follow the SBT installation instructions [here](https://www.scala-sbt.org/download.html).

To compile the code with SBT in a terminal outside your IDE/Editor environment, use this command:

    sbt compile

Building the code before the tutorial session will ensure that everything works.

### More about Using SBT

It's convenient to use SBT's interactive mode if you intend to run more than one "task". Type `sbt` and you'll see a prompt `sbt:akkaKafkaTutorial>` (`akkaKafkaTutorial` is the name of the top-level SBT project). Now you can run tasks like `tasks` like `clean`, `compile`, `package`, `help`, etc.

To list the most common tasks, run `tasks`. To see more complete lists, add the `-v` flag, or use `-V` to see all of them.

The interactive mode is also convenient if you want to work in one of the nested projects, rather than the top-level project. At the SBT prompt, enter `projects` to list the projects (which we'll discuss below), then use `project <name>` to switch to that subproject. Now use commands like `clean`, `compile`, etc.

> **Note:** If you get compilation errors when you run `sbt compile`, try starting `sbt`, then run `clean`, then `compile`. If it still fails, try running the tasks `project model` (switch to the model project), then `clean` and `compile`, then switch back to the top-level project `project akkaKafkaTutorial` and run `compile`.

You can use SBT inside IntelliJ, open the Terminal Tool Window and run SBT commands. There is also an SBT Tool Window that's useful for browsing the projects, the available tasks, etc.

To use the SBT build with other IDEs and editors, consult their documentation on using SBT and Scala. If your editor doesn't offer Scala and SBT support, just load the project as a Java project and use the editor to browse and edit the files. Use a terminal window to run SBT.

If you successfully built the code, you should be ready for the tutorial. We discuss running the services below.

## About the Project

Let's explore the tutorial code in more detail.

### Kafka

The examples rely on Kafka version 1.0 and leverage embedded Kafka servers. Using embedded Kafka servers, rather than standalone Kafka services, simplifies the setup and execution of the tutorial, but doesn't materially change how the examples work.

The implementations use two Kafka topics:

* `models_data` - for sending data to the applications
* `models_models` - for sending machine learning model updates, used for scoring the data

The model and data "provider" application, the `client` nested SBT project, will create an embedded Kafka server and the required topics. As a result, it has to be started before running any of the other services.

### Project Overview

The tutorial is organized as several SBT nested projects in subdirectories. Other subdirectories provide supporting functions, like the `data` directory:

Each of the following directories is used to create a service executable. Each is also is defined as a nested SBT project:

* `client` - Data and model loader used to run either the Akka Streams or Kafka Streams applications. This process has to be started first to ensure that the Kafka embedded server is running and the topics are created.
* `akkaStreamsCustomStage` - Akka Streams implementation of model serving using a custom stage.
* `akkaActorsPersistent` - Akka Actors and Akka Streams implementation of model serving that also provides state persistence to disk. This supports a production requirement to allow a failed service to be restarted from where it left off and it supports distributed scalability when using Akka cluster.
* `kafkaStreamsModelServerInMemoryStore` - Kafka Streams implementation of model serving using the [Processor Topology](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_processor_topology) API and in-memory storage. You'll see the package name "naive", because there is no durability if the state is stored only in memory.
* `kafkaStreamsModelServerKVStore` - Kafka Streams implementation of model serving using a key-value store provided by Kafka Streams and the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl).
* `kafkaStreamsModelServerCustomStore` - Kafka Streams implementation of model serving using a custom store and the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl).

The following directories provide shared code. They are also nested SBT projects:

* `configuration` - Shared configuration settings.
* `model` - Implementation of [TensorFlow](https://www.tensorflow.org/) and [PMML](https://en.wikipedia.org/wiki/Predictive_Model_Markup_Language) models.
* `protobufs` - [Google Protocol Buffers](https://github.com/google/protobuf) representation of both models and data.

You can see the list of SBT projects using the `sbt projects` command.  Note that the top level project, in the root directory, is called `akkaKafkaTutorial` and the SBT prompt is defined to be `sbt:akkaKafkaTutorial> `.

Finally, these directories provide other resources:

* `data` - Some data files for running the applications.
* `images` - Diagrams used for this document.

We provide Java and Scala implementations for all the examples, although we prefer [Scala](http://www.scala-lang.org/) for its expressiveness and concision. Because the same names are often used in both the Scala and Java implementations, we use the package naming convention `com.lightbend.scala` for (almost) all Scala code and `com.lightbend.java` for (almost) all Java code. An exception is the code generated in the `protobufs` directory, where no `scala` or `java` package names are used. If you're new to Scala, this naming scheme is our convention; it's not commonly used in projects.

### Application Architecture

A high level view of the overall model serving architecture is as follows:

![Overall architecture of model serving](images/OverallModelServing.png)

It is similar to this [dynamically controlled stream](https://data-artisans.com/blog/bettercloud-dynamic-alerting-apache-flink), as described for [Apache Flink](https://flink.apache.org).

This architecture assumes there are two data streams - one containing data that needs to be scored, and one containing the model updates. The streaming engine contains the current model used for the actual scoring in memory. The results of scoring can be either delivered downstream or used by the streaming engine internally as a new stream, i.e., as input for additional calculations.

If there is no model currently defined, the input data is dropped. When the new model is received, it is instantiated in memory. Once instantiation is complete, scoring is switched to the new model.

The model stream can either contain the binary blob of the data itself ("pass by value") or the reference to the model data stored externally ("pass by reference") in a database or a file system, like HDFS or S3. Only pass by value is implemented in the code.

This approach effectively implements model scoring as a new type of functional transformation, which can be used by any other stream transformations.

Although the overall architecture shown above is represented as a single model, a single streaming engine could score with multiple models simultaneously. This enhancement is left as an exercise for the student ;)

### Queryable State

Kafka Streams recently introduced [queryable state](https://docs.confluent.io/current/streams/developer-guide.html#id8), which is a nice approach for execution monitoring and extracting the state of a stream. This feature allows you to treat the stream processing layer as a lightweight embedded database and, more concretely, to directly query the latest state of your stream processing application, without needing to materialize that state to external databases or external storage first.

![Queriable state](images/QueryableState.png)

Both the Akka Streams and Kafka Streams implementations support queryable state. Here, we use [Akka HTTP](https://doc.akka.io/docs/akka-http/current/scala/http/) for Web based access to both the Akka Streams and Kafka Streams queryable APIs.

## Running the Application

We listed the SBT projects that have service executables above. Now let's explore these executables and how to run them using SBT and IDEs.

First, if you using SBT from a terminal, to run a particular executable, use the following convention that specifies the SBT project, the `client` in this example, then select the executable to run from the list presented:

```
sbt:akkaKafkaTutorial> client/run

Multiple main classes detected, select one to run:

 [1] com.lightbend.scala.kafka.client.DataProvider
 [2] com.lightbend.scala.kafka.client.DataReader

Enter number: 1
...
```

If you know the fully qualified name of a specific executable, for example the `DataProvider` just shown, you can run it using `runMain`:

```
sbt:akkaKafkaTutorial> client/runMain com.lightbend.scala.kafka.client.DataProvider
...
```

> **Note:** You will need one terminal for _each_ service executed concurrently.

If you are using IntelliJ or another IDE, just right click on the `client` project `com.lightbend.scala.kafka.client.DataProvider` file and select _Run_ (or _Debug_). If the file is open in the editor, just right click there to run it. There is also a _Run_ menu for the IDE.

Now let's look at the specific executables, starting with the `client`, since it has to be executed first to set up the embedded Kafka instance. Then we'll list the rest of the projects alphabetically. We'll focus on the classes (Scala and Java types) that provide the executable `main` routines. We won't discuss the other classes here. We'll discuss them verbally during the tutorial session.

### Client

The client service that starts the embedded Kafka server, creates the topics, and publishes data to them. Hence, the client corresponds to the _Data Source_ and _Model Source_ in the first architecture diagram above.

| | | |
| :- | :- | :- |
| SBT Project | `client` | |
| Directory   | `client` | |
| Data Provider | [com.lightbend.scala.kafka.client.DataProvider](client/src/main/scala/com/lightbend/scala/kafka/client/DataProvider.scala) | (Scala only) Provider service |
| Data Reader | [com.lightbend.scala.kafka.client.DataReader](client/src/main/scala/com/lightbend/scala/kafka/client/DataReader.scala) | Verifies that messages are published correctly |

In this case, we only provide Scala implementations, as this service isn't the focus of this tutorial.

The `DataProvider` is a data and model publisher. This executable contains an embedded Kafka server and has to be started first to ensure that the embedded Kafka server is running and the necessary topics are created.

Start the `DataProvider` using SBT as shown above:

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
```

_Or_ directly from the terminal command line, e.g., bash (prompt not show):

```bash
sbt "client/runMain com.lightbend.scala.kafka.client.DataProvider"
```

The quotes are required!

_Or_ use one of the _Run_ or _Debug_ menu items in your IDE for the `client` project, `com.lightbend.scala.kafka.client.DataProvider`.

While `DataProvider` runs, you'll see output to the terminal like this:

```
...
printed 280 records
printed 290 records
Published Model generated from SparkML
printed 300 records
printed 310 records
printed 320 records
printed 330 records
...
```

Note the occasional messages about updates from SparkML!

You can run the `DataReader`, if desired, the same way. It reads the Kafka topics to which `DataProvider` is writing, to verify that data is being successfully written to the topics.

The `DataReader` outputs messages like the following:

```
...
Retrieved message #1: key = null, value = [B@2723c85, size = 105, first 5 elements = [9,102,102,102,102]
Retrieved message #2: key = null, value = [B@13046ba0, size = 105, first 5 elements = [9,102,102,102,102]
Retrieved message #3: key = null, value = [B@7a588539, size = 105, first 5 elements = [9,102,102,102,102]
Retrieved message #4: key = null, value = [B@7e496408, size = 105, first 5 elements = [9,-102,-103,-103,-103]
Retrieved message #5: key = null, value = [B@6d4cf950, size = 105, first 5 elements = [9,-102,-103,-103,-103]
...
```

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

(Note that `modelserver` is used for the Java version and `modelServer` for the Scala version...)

Start one of the applications by running `sbt akkaStreamsCustomStage/run` in a terminal window, then select the executable to run. Or use `runMain`, e.g., for the Scala version:

```
sbt:akkaKafkaTutorial> akkaStreamsCustomStage/runMain com.lightbend.scala.modelServer.modelServer.AkkaModelServer
```

For the Java version:

```
sbt:akkaKafkaTutorial> akkaStreamsCustomStage/runMain com.lightbend.java.modelserver.modelserver.AkkaModelServer
```

Directly from the terminal prompt (prompt not show), Scala and Java versions:

```bash
sbt "akkaStreamsCustomStage/runMain com.lightbend.scala.modelServer.modelServer.AkkaModelServer"
sbt "akkaStreamsCustomStage/runMain com.lightbend.java.modelserver.modelserver.AkkaModelServer"
```

_Or_ use one of the _Run_ or _Debug_ menu items in your IDE for the `akkaStreamsCustomStage` project, `com.lightbend.scala.modelServer.modelServer.AkkaModelServer` (Scala) or `com.lightbend.java.modelserver.modelserver.AkkaModelServer` (Java).

You'll see output like the following, where at first it skips processing, because it hasn't received a model yet, and then it receives a "model to serve":

```
...
No model available - skipping
New model - ModelToServe(winequalityDesisionTreeRegression,generated from SparkML,PMML,[B@471244ec,wine)
Calculated quality - 5.67910447761194 calculated in 76 ms
New model - ModelToServe(winequalityMultilayerPerceptron,generated from SparkML,PMML,[B@72ea5ccb,wine)
Calculated quality - 6.0 calculated in 48 ms
New model - ModelToServe(winequalityGeneralizedLinearRegressionGamma,generated from SparkML,PMML,[B@48d97260,wine)
Calculated quality - 6.1631297415506 calculated in 16 ms
New model - ModelToServe(winequalityRandonForrestClassification,generated from SparkML,PMML,[B@51ddd2e1,wine)
Calculated quality - 6.0 calculated in 34 ms
Calculated quality - 6.0 calculated in 1 ms
Calculated quality - 6.0 calculated in 1 ms
Calculated quality - 5.0 calculated in 1 ms
Calculated quality - 6.0 calculated in 2 ms
...
```

Once either server is running, go to http://localhost:5500/state to obtain the current state of execution, which in this case is the current model used for scoring.

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
| Java Version  | [com.lightbend.java.modelserver.actor.modelServer.AkkaModelServer](akkaActorsPersistent/src/main/java/com/lightbend/java/modelserver/actor/modelserver/AkkaModelServer.java) |

Start one of the applications by running `sbt akkaActorsPersistent/run` in a terminal window, then select the executable to run. Or use `runMain`, e.g., for the Scala and Java versions:

```
sbt:akkaKafkaTutorial> akkaActorsPersistent/runMain com.lightbend.scala.modelserver.actor.modelserver.AkkaModelServer
sbt:akkaKafkaTutorial> akkaActorsPersistent/runMain com.lightbend.java.modelserver.actor.modelServer.AkkaModelServer
```

Directly from the terminal prompt (prompt not show), Scala and Java versions:

```bash
sbt "akkaActorsPersistent/runMain com.lightbend.scala.modelserver.actor.modelserver.AkkaModelServer"
sbt "akkaActorsPersistent/runMain com.lightbend.java.modelserver.actor.modelServer.AkkaModelServer"
```

_Or_ use one of the _Run_ or _Debug_ menu items in your IDE for the `akkaActorsPersistent` project, `com.lightbend.scala.modelserver.actor.modelserver.AkkaModelServer` (Scala) or `com.lightbend.java.modelserver.actor.modelServer.AkkaModelServer` (Java).

Once running, visit these links:

* http://localhost:5500/models for information about the currently-used models
* http://localhost:5500/state/"a_model" for the current state of execution for `a_model`, as listed in the previous link. For example, use `wine` for the model.

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

Start one of the applications by running `sbt kafkaStreamsModelServerInMemoryStore/run` in a terminal window, then select the executable to run. Or use `runMain`, e.g., for the Scala and Java versions:

```
sbt:akkaKafkaTutorial> kafkaStreamsModelServerInMemoryStore/runMain com.lightbend.scala.naive.modelserver.ModelServer
sbt:akkaKafkaTutorial> kafkaStreamsModelServerInMemoryStore/runMain com.lightbend.java.naive.modelserver.ModelServer
```

Directly from the terminal prompt (prompt not show), Scala and Java versions:

```bash
sbt "kafkaStreamsModelServerInMemoryStore/runMain com.lightbend.scala.naive.modelserver.ModelServer"
sbt "kafkaStreamsModelServerInMemoryStore/runMain com.lightbend.java.naive.modelserver.ModelServer"
```

_Or_ use one of the _Run_ or _Debug_ menu items in your IDE for the `kafkaStreamsModelServerInMemoryStore` project,
`com.lightbend.scala.naive.modelserver.ModelServer` (Scala)
or `com.lightbend.java.naive.modelserver.ModelServer` (java).

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

Start one of the applications by running `sbt kafkaStreamsModelServerKVStore/run` in a terminal window, then select the executable to run. Or use `runMain`, e.g., for the Scala and Java versions:

```
sbt:akkaKafkaTutorial> kafkaStreamsModelServerKVStore/runMain com.lightbend.scala.standard.modelserver.ModelServer
sbt:akkaKafkaTutorial> kafkaStreamsModelServerKVStore/runMain com.lightbend.scala.standard.modelserver.ModelServerFluent
sbt:akkaKafkaTutorial> kafkaStreamsModelServerKVStore/runMain com.lightbend.java.standard.modelserver.ModelServer
```

Directly from the terminal prompt (prompt not show), Scala and Java versions:

```bash
sbt "kafkaStreamsModelServerKVStore/runMain com.lightbend.scala.standard.modelserver.ModelServer"
sbt "kafkaStreamsModelServerKVStore/runMain com.lightbend.scala.standard.modelserver.ModelServerFluent"
sbt "kafkaStreamsModelServerKVStore/runMain com.lightbend.java.standard.modelserver.ModelServer"
```

_Or_ use one of the _Run_ or _Debug_ menu items in your IDE for the `kafkaStreamsModelServerKVStore` project,
`com.lightbend.scala.standard.modelserver.scala.ModelServer`,
`com.lightbend.scala.standard.modelserver.scala.ModelServerFluent`,
or `com.lightbend.java.standard.modelserver.ModelServer`.

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

Start one of the applications by running `sbt kafkaStreamsModelServerCustomStore/run` in a terminal window, then select the executable to run. Or use `runMain`, e.g., for the Scala and Java versions:

```
sbt:akkaKafkaTutorial> kafkaStreamsModelServerCustomStore/runMain com.lightbend.scala.custom.modelserver.ModelServer
sbt:akkaKafkaTutorial> kafkaStreamsModelServerCustomStore/runMain com.lightbend.scala.custom.modelserver.ModelServerFluent
sbt:akkaKafkaTutorial> kafkaStreamsModelServerCustomStore/runMain com.lightbend.java.custom.modelserver.ModelServer
```

Directly from the terminal prompt (prompt not show), Scala and Java versions:

```bash
sbt "kafkaStreamsModelServerCustomStore/runMain com.lightbend.scala.custom.modelserver.ModelServer"
sbt "kafkaStreamsModelServerCustomStore/runMain com.lightbend.scala.custom.modelserver.ModelServerFluent"
sbt "kafkaStreamsModelServerCustomStore/runMain com.lightbend.java.custom.modelserver.ModelServer"
```

_Or_ use one of the _Run_ or _Debug_ menu items in your IDE for the `kafkaStreamsModelServerCustomStore` project,
`com.lightbend.scala.custom.modelserver.ModelServer`,
`com.lightbend.scala.custom.modelserver.ModelServerFluent`,
or `com.lightbend.java.custom.modelserver.ModelServer`.

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

The tutorial presentation will discuss other considerations when scaling these microservices.

## References

### Scala

* [Scala web site](https://www.scala-lang.org/)
* [Scaladocs](https://www.scala-lang.org/api/current/index.html) (This is for 2.12.4; we're using 2.11.11, but close enough...)

### Kafka

* [Kafka](https://kafka.apache.org/)
* [Kafka Documentation](https://kafka.apache.org/documentation/)

### Akka and Akka Streams

* [Akka](https://akka.io)
* [Akka Documentation](https://akka.io/docs)
* Akka Streams (Scala):
    * [Reference](https://doc.akka.io/docs/akka/current/stream/index.html?language=scala)
    * [Scaladocs](https://doc.akka.io/api/akka/current/akka/stream/index.html)
* Akka Streams (Java):
    * [Reference](https://doc.akka.io/docs/akka/current/stream/index.html?language=java)
    * [Javadocs](https://doc.akka.io/japi/akka/current/index.html?akka/stream/package-summary.html)
* Miscellaneous:
    * Integrating Akka Streams and Akka Actors: [Part I](http://blog.colinbreck.com/integrating-akka-streams-and-akka-actors-part-i/), [Part II](http://blog.colinbreck.com/integrating-akka-streams-and-akka-actors-part-ii/)
    * [Akka Team Blog](https://akka.io/blog/)

### Kafka Streams

* [Kafka Streams](https://kafka.apache.org/10/documentation/streams/)
* Kafka Streams API:
    * [Javadocs](https://kafka.apache.org/10/javadoc/index.html?org/apache/kafka/streams/KafkaStreams.html)
    * [Scaladocs (2.12)](https://developer.lightbend.com/docs/api/kafka-streams-scala/0.1.2/com/lightbend/kafka/scala/streams/) - for the new Lightbend Scala API
    * [Scaladocs (2.11)](https://developer.lightbend.com/docs/api/kafka-streams-scala_2.11/0.1.2/#package)
* Miscellaneous:
    * [Enabling Exactly-Once in Kafka Streams](https://www.confluent.io/blog/enabling-exactly-kafka-streams/)

### For More Information

Interested in an integrated and commercially supported distribution of Akka Streams, Kafka Streams, and Kafka, plus other tools like Spark, Flink, and HDFS? See the https://www.lightbend.com/products/fast-data-platform for more information about the Lightbend Fast Data Platform.

