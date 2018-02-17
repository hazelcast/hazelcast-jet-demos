# Market Data Analyzer

This is an interactive demo showing both how Jet can be used to easily import data into Hazelcast IMDG,
and to analyse the data as it changes.

## Market Data

"_Market Data_" is a term normally associated with the stock markets. These markets are external both to the
application, and usually also to the organisation.

In the stock market, there are various markets for various things - bonds, equities, currencies, etc. The market
data itself is information about activities, the prices items are buying and selling for, volumes being
traded, and so on.

As an organisation, we would usually _pay_ for a real-time feed of this market data, and wish to make the
best value for money from the information.

This market data feed will arrive via some external interface into our organisation, and would be highly
unlikely to be in the format that suits us best. A bit of reformatting might be required.

### Precious Metals

To keep this example simple, we are going to focus on the market for precious metals. The big four
are Gold, Silver, Platinum and Palladium. The first three are used in jewelry, with Palladium being
a key component in the catalytic converters for combustion engine vehicles such as cars.

Furthermore, we shall simplify to hold a current price for each of these four. The reality would
be to hold several prices for each commodity, a _buy_ price, a _sell_ price, daily highs, daily
lows, opening prices, closing prices, and perhaps others.

## The application

The application here consists of three parts.

[Apache Kafka](https://kafka.apache.org/) is used as the market data feed, acting as the transport
for the precious metal prices from the outside world to our application. Kafka is picked here as a
popular transport mechanism, but anything else would be fine also.

There is a `kafka-writer` module that generates random market data and writes it in to Kafka.
This similates the real market data feed. If you have a real market data feed, you can extend
the application to use that instead.

Finally, there is a Hazelcast grid, a collection of In-Memory Data Grid [IMDG](https://hazelcast.org/)
processes each of which embeds a [Jet](https://jet.hazelcast.org/) processing engine. Embedding
Jet in the same process as IMDG is the easiest way to connect the two rather than have them
in separate JVMs.

## Running the example

This is an interactive example, the JVMs hosting IMDG & Jet respond to commands from the command
line.

The sample output below assumes there is one Zookeeper process, three Kafka processes, and
three IMDG/Jet processes.

### Kafka

This example assumes that Kafka is running on the current machine, and is controlled
via some provided scripts.

Kafka 1.0.0, released 1st November 2017, is the current stable version at the time of writing,
and can be obtained from [here](https://kafka.apache.org/downloads). The version used is
for Scala 2.11 not 2.12, as the former is more widespread.

Assuming Kafka has been installed in `/Applications/kafka_2.11-1.0.0`, there are five
command scripts in `src/main/scripts` to run in this sequence.

1. `start-zookeeper.sh` 
This script will start a single Zookeeper configuration server, running on port 2181 on the current machine.

1. `start-kafka-0.sh`
This script will start a first Kafka broker, running on port 9092 on the current machine.

1. `start-kafka-1.sh`
This script will start a second Kafka broker, running on port 9093 on the current machine.

1. `start-kafka-2.sh`
This script will start a third Kafka broker, running on port 9094 on the current machine.

1. `print-topic.sh`
This script will first create a partitioned Kafka topic called "_precious_" if this does not
exist. Then it will listen for data being written to this topic and echo it to the screen.
It will not stop until interrupted with _Ctrl-C_.

* Other scripts

⋅⋅⋅Some other scripts are also provided

⋅⋅⋅* `clean-topic.sh` This will shut down Kafka and Zookeeper, and remove data from the topics.

⋅⋅⋅* `stop-kafka.sh` This will stop the Kafka brokers but leave Zookeeper running.

⋅⋅⋅* `stop-zookeeper.sh` This will stop Zookeeper, and should be run after Kafka brokers are stopped.



This will s



### Kafka Writer

### Hazelcast

## Jet

## Summary

Kafka is partitioned, Hazelcast is partitioned. Ingest from Kafka to Hazelcast is both easy coding
and efficient coding. Jet upload from Kafka runs in parallel, each node reads from some Kafka
partitions, minimising data cross-transfer on the network.

Jet can merge data into IMDG data structures, here [IMap](http://docs.hazelcast.org/docs/3.9.3/javadoc/com/hazelcast/core/IMap.html)
is used. This writing to IMDG can operate in parallel across the grid, minimising data cross-transfer on the network.

Traditional IMDG time-series analysis requires the input to be stored. Time-series analysis with Jet
operates on a memory stream, it is the output that is stored. Reducing the storage need for intermediate
data gives faster calculations and cost savings.

