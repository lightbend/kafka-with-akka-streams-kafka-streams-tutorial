# Building Kafka-based Microservices with Akka Streams and Kafka Streams - a Tutorial

[![Join the chat at https://gitter.im/kafka-with-akka-streams-kafka-streams-tutorial](https://badges.gitter.im/kafka-with-akka-streams-kafka-streams-tutorial.svg)](https://gitter.im/kafka-with-akka-streams-kafka-streams-tutorial?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

> **NOTE:** We strongly recommend using Java 8 with this tutorial. There is an experimental branch `latest-dependency-versions` that compiles with Java 9, but still has problems running successfully.

[Boris Lublinsky](mailto:boris.lublinsky@lightbend.com) and [Dean Wampler](mailto:dean.wampler@lightbend.com), [Lightbend](https://lightbend.com/fast-data-platform)

* [Software Architecture Conference New York, Monday, February 26, 2018](https://conferences.oreilly.com/software-architecture/sa-ny/public/schedule/detail/63999)
* [Strata Data Conference San Jose, Tuesday, March 6, 2018](https://conferences.oreilly.com/strata/strata-ca/public/schedule/detail/63983)
* [Strata Data Conference London, Tuesday, May 22, 2018](https://conferences.oreilly.com/strata/strata-eu/public/schedule/detail/65420)
* [OSCON, Opensource convention, Portland, Monday, July 16, 2018](https://conferences.oreilly.com/oscon/oscon-or/public/schedule/detail/67531)
* [Strata Data Conference NYC, Tuesday, September 11, 2018](https://conferences.oreilly.com/strata/strata-ny/public/schedule/detail/68931)

©Copyright 2018, Lightbend, Inc. Apache 2.0 License. Please use as you see fit, but attribution is requested.

This tutorial provides an introduction to streaming data microservices using Kafka with Akka Streams and Kafka Streams. Hence, the tutorial helps you compare and contrast these streaming libraries for your own use.

See the companion presentation for the tutorial:

* `Kafka-with-Akka-Streams-Kafka-Streams-Tutorial.key` Keynote file
* `Kafka-with-Akka-Streams-Kafka-Streams-Tutorial.pdf` for non-Mac users ;)
* `Kafka-with-Akka-Streams-Kafka-Streams-Tutorial-with-notes.pdf` with speaker notes

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

> **WARNING:** Unfortunately, the IntelliJ build doesn't properly build the `protobuf` project (TBD), so do a one-time command-line build as follows:

1. Open the _sbt shell_ tool window in IntelliJ (e.g., _View > Tool Windows > sbt shell_).
2. Type `package`, once it's finished loading.
3. It should end with `[success] Total time: 30 s, completed Feb ...` after ~30 seconds
4. Now just use IntelliJ's _Build_ command as needed or automatically

> **Note:** There is also an `sbt` tool window that's useful for browsing the project structure, including the defined _tasks_ (commands). You can double click a task to run it.

If you don't have a GitHub account, just download the latest [release](https://github.com/lightbend/kafka-with-akka-streams-kafka-streams-tutorial/releases) and import the code as an SBT project into your IDE. In IntelliJ, use these steps:

* _Import Project_
* Select the project root directory (i.e., the same as for this README)
* Select `sbt` as the project type
* Use the default settings for `sbt`. Use JDK 1.8 if it's not shown as the default.
* Profit!!

If you wish to use SBT in a terminal (e.g., in conjunction with your text editor), follow the SBT installation instructions [here](https://www.scala-sbt.org/download.html).

To compile the code with SBT in a terminal outside your IDE/Editor environment, use this command:

    sbt package

This _task_ compiles the Java and Scala sources, then packages the class and resource files (such as `log4j.properties`) into jar files.

> **Tip:** It's actually sufficient to just run `sbt compile`, but then when you run the apps, you'll get warnings about missing `log4j` configurations.

Building the code before the tutorial session will ensure that everything works.

### More about Using SBT

It's convenient to use SBT's interactive mode if you intend to run more than one "task". Type `sbt` and you'll see a prompt `sbt:akkaKafkaTutorial>` (`akkaKafkaTutorial` is the name of the top-level SBT project). Now you can run tasks like `tasks` like `clean`, `compile`, `package`, `help`, etc.

To list the most common tasks, run `tasks`. To see more complete lists, add the `-v` flag, or use `-V` to see all of them.

The interactive mode is also convenient if you want to work in one of the nested projects, rather than the top-level project. At the SBT prompt, enter `projects` to list the projects (which we'll discuss below), then use `project <name>` to switch to that subproject. Now use commands like `clean`, `compile`, `package`, etc.

> **Note:** If you get compilation errors when you run `sbt package` (or `sbt compile`), try starting the SBT interpreter with just `sbt`, then run `clean`, then `package`. If it still fails, try running the tasks `project model` (switch to the model project), then `clean` and `package`, then switch back to the top-level project `project akkaKafkaTutorial` and run `package`.

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

An application called the `client` pumps data into these topics. It corresponds to the `client` SBT subproject. It creates an embedded Kafka server and the required topics. As a result, it has to be started before running any of the other services.

### Project Overview

The tutorial is organized as several SBT nested projects in subdirectories. Other subdirectories provide supporting functions, like the `data` directory:

Each of the following directories is used to create a service executable. Each is also is defined as a nested SBT project:

* `client` - Data and model loader used to with both the Akka Streams and Kafka Streams applications. This process has to be started first to ensure that the Kafka embedded server is running and the topics are created.
* `akkaStreamsModelServer` - Akka Streams implementation of model serving using a combination of _Akka Streams_ and _Akka Actors_. The implementation demonstrates two implementation options for model serving (i.e., scoring data) in the stream:
    1. Send messages to model-serving Akka Actors (`ActorModelServerProcessor`)
    1. Use a custom Akka Streams _Stage_ to incorporate model serving (`CustomStageModelServerProcessor`)
* `kafkaStreamsModelServer` - Kafka Streams implementation of model serving using two approaches for defining the stream and three approaches for persisting state. Rather than implement all six combinations, we provide three:
    1. An in-memory state store (not very durable...). This example uses the Kafka Streams [Processor Topology](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_processor_topology) API (`MemoryStoreStreamBuilder`)
    2. A "standard" store, which is the key-value store provided by Kafka Streams. This example uses the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl) (`StandardStoreStreamBuilder`)
    3. A "custom" store, showing how to write your own. This example also uses the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl) (`CustomStoreStreamBuilder`)

In addition the following directories (and SBT subprojects) provide shared code:

* `configuration` - Shared configuration settings.
* `model` - Implementation of [TensorFlow](https://www.tensorflow.org/) and [PMML](https://en.wikipedia.org/wiki/Predictive_Model_Markup_Language) models.
* `protobufs` - [Google Protocol Buffers](https://github.com/google/protobuf) representation of both models and data.

You can see the list of SBT projects using the `sbt projects` command or the _SBT Tool_ window in IntelliJ.  Note that the top level project, corresponding to the root directory, is called `akkaKafkaTutorial` and the SBT prompt is defined to be `sbt:akkaKafkaTutorial>`.

Finally, these directories provide other resources:

* `data` - Some data files for running the applications
* `images` - Diagrams used for this document
* `presentation` - The tutorial slide deck (Keynote and PDF)
* `project` - Used by SBT

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

Now let's look at the specific executables, starting with the `client`, since it has to be executed first to set up the embedded Kafka instance. Then we'll discuss the other projects. We'll focus on the classes (Scala and Java types) that provide the executable `main` routines. We won't discuss the other classes here. We'll discuss them verbally during the tutorial session.

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

### Akka Streams Model Server

| | |
| :- | :- |
| SBT Project | `akkaStreamsModelServer` |
| Directory   | `akkaStreamsModelServer` |
| Scala Version | [com.lightbend.scala.akkastream.modelserver.AkkaModelServer](akkaStreamsModelServer/src/main/scala/com/lightbend/scala/akkastream/modelserver/AkkaModelServer.scala) |
| Java Version  | [com.lightbend.java.akkastream.modelserver.AkkaModelServer](akkaStreamsModelServer/src/main/java/com/lightbend/java/akkastream/modelserver/AkkaModelServer.java) |

There are two options for model serving based on Akka Streams (with both Scala and Java implementations). You specify which one with the command-line options shown:

1. An implementation based on the usage of a custom "stage", `CustomStageModelServerProcessor`, which is a fully type-safe way to encapsulate the required functionality. Use the command-line argument `c` or `custom`. However, this is also the default choice.
1. An implementation based on Akka actors and the [router pattern](http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/), `ActorModelServerProcessor`. Use the command-line argument `a` or `actor`.

There is also a `-h` or `--help` option for help.

The custom stage implementation uses the stream processor functionality from the overall architecture diagram shown above. The overall implementation looks as follows:

![Akka streams custom stage model serving](images/AkkaStreamsJoin.png)

The Akka Actor-based implementation uses the [router pattern](http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/) to delegate to a pool of actors for scoring. This is a scalability mechanism.

Scoring is implemented by an model-serving actor, while a model-serving manager serves as the router.

We discuss this implementation in more detail below; it also includes a built-in web server for viewing status information.

#### Run the Application

Start one of the applications by running `sbt akkaStreamsModelServer/run` in a terminal window, then select the executable to run (Java or Scala version). Or use `runMain`, e.g., for the Scala version:

```
sbt:akkaKafkaTutorial> akkaStreamsModelServer/runMain com.lightbend.scala.akkastreams.modelserver.AkkaModelServer
```

For the Java version:

```
sbt:akkaKafkaTutorial> akkaStreamsModelServer/runMain com.lightbend.java.akkastreams.modelserver.AkkaModelServer
```

You can also pass any of the command-line options mentioned above. Try `-h` to see the help, after you select the number for the executable to run.

At the terminal prompt (prompt not show), you can invoke the Scala or Java version as follows:

```bash
sbt "akkaStreamsModelServer/runMain com.lightbend.scala.akkastreams.modelserver.AkkaModelServer"
sbt "akkaStreamsModelServer/runMain com.lightbend.java.akkastreams.modelserver.AkkaModelServer"
```

(Quotes required!)

_Or_ use one of the _Run_ or _Debug_ menu items in your IDE for the `akkaStreamsModelServer` project, `com.lightbend.scala.akkastreams.modelserver.AkkaModelServer` (Scala) or `com.lightbend.java.akkastreams.modelserver.AkkaModelServer` (Java).

> **Tip:** It's less convenient to use command-line options when invoking from the IDE. It might be easiest to edit the `AkkaModelServer` implementation you are running and change which default it uses. For example, in the Scala version, look for the line `case Nil => CustomStageModelServerProcessor`.

Which ever technique you use, you'll see output like the following, where at first it skips processing, because it hasn't received a model yet, and then it receives a "model to serve":

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

#### Using Akka Persistence to Save Model State

When you use the `ActorModelServerProcessor` option (`a` or `actor`), it writes the state to the local file system using [Akka Persistence](https://doc.akka.io/docs/akka/2.5.4/scala/persistence.html), which insures that the application can be restarted if it crashes. Hence, this feature helps support the resiliency required for production systems. This data is written to the `persistence` directory. This code is implemented in `com.lightbend.scala.akkastream.modelserver.actors.ModelServingActor`.

A built-in Akka HTTP server provides access to state information. Once running, visit these links:

* http://localhost:5500/models for information about the currently-used models
* http://localhost:5500/state/wine for the current state of execution for the `wine` model that's listed by the previous link.

## Kafka Streams Model Server

| | |
| :- | :- |
| SBT Project | `kafkaStreamsModelServer` |
| Directory   | `kafkaStreamsModelServer` |
| Scala Version | [com.lightbend.scala.kafkastreams.modelserver.ModelServer](kafkaStreamsModelServer/src/main/scala/com/lightbend/scala/kafkastreams/modelserver/ModelServer.scala) |
| Java Version | [com.lightbend.java.kafkastreams.modelserver.ModelServer](kafkaStreamsModelServer/src/main/java/com/lightbend/java/kafkastreams/modelserver/ModelServer.java) |

As discussed above, there are three versions of model serving provided using Kafka Streams, with implementations in both Scala and Java. When you invoke the `KafkaModelServer` executable, you specify the command line arguments shown to pick the implementation:

1. An in-memory state store (not very durable...). This example uses the Kafka Streams [Processor Topology](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_processor_topology) API. The `MemoryStoreStreamBuilder` type encapsulates this logic. Use the command-line argument `m` or `memory` to select it.
2. A "standard" store, which is the key-value store provided by Kafka Streams. This example uses the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl). The `StandardStoreStreamBuilder` type encapsulates this logic. Use the command-line argument `s` or `standard` to select it.
3. A "custom" store, showing how to write your own. This example also uses the Kafka Streams [DSL](https://kafka.apache.org/10/documentation/streams/developer-guide#streams_dsl) (`CustomStoreStreamBuilder`) The `CustomStoreStreamBuilder` type encapsulates this logic. Use the command-line argument `c` or `custom` to select it. However, this is also the default choice.

See [this blog post](http://mkuthan.github.io/) for a discussion of the differences between the Kafka Streams Processor Topology and the DSL.

For the Scala implementations based on Kafka Stream DSL, we are using the new Scala wrapper for the Java DSL created by Lightbend and recently donated to the Kafka project: [Scala API for Kafka Streams](https://github.com/lightbend/kafka-streams-scala) that Lightbend recently open sourced. It provides an idiomatic Scala version of the fluent Java Kafka Streams API.

See also the [Kafka Streams Query API](https://github.com/lightbend/kafka-streams-query), which provides a convenient way to query stream state across a cluster. (not used in this tutorial)

The model serving implementation is illustrated here:

![Kafka streams model serving](images/KafkaStreamsJoin.png)

#### Run the Application

> **Note:** Recall that you can only run one of the streaming applications at a time. Make sure the Akka versions have been stopped.

As for the Akka Streams example, you can run this application several ways. If desired, pass one of the arguments to select the implementation.

Start one of the Scala or Java applications by running `sbt kafkaStreamsModelServer/run` in a terminal window, then select the executable to run. Or use `runMain`, e.g., for the Scala and Java versions:

```
sbt:akkaKafkaTutorial> kafkaStreamsModelServer/runMain com.lightbend.scala.kafkastreams.modelserver.KafkaModelServer
sbt:akkaKafkaTutorial> kafkaStreamsModelServer/runMain com.lightbend.java.kafkastreams.modelserver.KafkaModelServer
```

Directly from the terminal prompt (prompt not show), Scala and Java versions:

```bash
sbt "kafkaStreamsModelServer/runMain com.lightbend.scala.kafkastreams.modelserver.KafkaModelServer"
sbt "kafkaStreamsModelServer/runMain com.lightbend.java.kafkastreams.modelserver.KafkaModelServer"
```

_Or_ use one of the _Run_ or _Debug_ menu items in your IDE for the `kafkaStreamsModelServer` project,
`com.lightbend.scala.kafkastreams.modelserver.KafkaModelServer` (Scala)
or `com.lightbend.java.kafkastreams.modelserver.KafkaModelServer` (Java).

> **Tip:** As above, it's less convenient to use command-line options when running from an IDE menu. You can change the default instead in `KafkaModelServer`. In the Scala version, look for the `case Nil =>`.

Once running, visit the following URLs:

* http://localhost:8888/state/value (all versions) for the current state of execution for a given model
* http://localhost:8888/state/instances (custom and standard stores only) for the list of currently deployed instances

## Exercises

We have embedded a few code comments with exercises. Search for the comment `// Exercise`. They suggest ways you can extend the functionality and thereby practice using the APIs. (Solutions are not yet provided; TODO)

## Production Considerations

The examples illustrate how to implement a model-serving microservice using Akka Streams and Kafka Streams in various ways. A few illustrate other production considerations, such as persisting state so that restarts can pick up where processing left off.

Let's finish by considering a few other production considerations.

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
    * [Colin Breck's blog](http://blog.colinbreck.com/), such as his two-part series on integrating Akka Streams and Akka Actors: [Part I](http://blog.colinbreck.com/integrating-akka-streams-and-akka-actors-part-i/), [Part II](http://blog.colinbreck.com/integrating-akka-streams-and-akka-actors-part-ii/)
    * [Akka Team Blog](https://akka.io/blog/)

### Kafka Streams

* [Kafka Streams](https://kafka.apache.org/10/documentation/streams/)
* Kafka Streams API:
    * [Javadocs](https://kafka.apache.org/10/javadoc/index.html?org/apache/kafka/streams/KafkaStreams.html)
    * [Scaladocs (2.12)](https://developer.lightbend.com/docs/api/kafka-streams-scala/0.1.2/com/lightbend/kafka/scala/streams/) - for the new Lightbend Scala API
    * [Scaladocs (2.11)](https://developer.lightbend.com/docs/api/kafka-streams-scala_2.11/0.1.2/#package)
* Miscellaneous:
    * [Confluent blog](https://www.confluent.io/blog/), such as [Enabling Exactly-Once in Kafka Streams](https://www.confluent.io/blog/enabling-exactly-kafka-streams/)

### For More Information

Interested in an integrated and commercially supported distribution of Akka Streams, Kafka Streams, and Kafka, plus other tools like Spark, Flink, and HDFS? See the https://www.lightbend.com/products/fast-data-platform for more information about the Lightbend Fast Data Platform.

