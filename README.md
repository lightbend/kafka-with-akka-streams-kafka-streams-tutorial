# Kafka Streams Model server

This is a simple implementation of model serving using Kafka Streams
The basic idea behind this implementation is fairly straightforward - there are two streams:

-**Data Stream** - Kafka stream delivering data record as protobuf buffer (example, modeldescriptor.proto)

-**Model Stream** - Kafka stream delivering models as protobuf buffer (example, modeldescriptor.proto)
