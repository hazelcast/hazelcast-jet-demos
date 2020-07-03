# Real-time dto.Trade Monitor

This demo shows how one can generate a structured view of real-time
trades via using aggregations of the Jet. Hazelcast Jet computes complex
analytical and ad hoc queries with a rapid execution time. In this
example, we run 3 different jobs that performs followings:

* Load static business data into IMap.
* Ingests trades from Apache Kafka into a distributed map.
* Performs an aggregation on the trades, storing the results into a
  separate map.

These two maps are utilized by a live dashboard which offers drill down
functionality into viewing individual trades that make up the
aggregation.

## How to run

### Build the project

To build and package the application, run:

```bash
mvn package
```

### Prepare The Environment

After building the application, prepare the dependencies for running the demo.

#### Create A Kafka Topic

Create a topic named "trades" with four partitions and one replica:

```bash
kafka-topics --create --replication-factor 1 --partitions 4 --topic trades --bootstrap-server localhost:9092
```

#### Start The Kafka Producer

Start the trade producer application that creates trades and send them out as messages to the `trades` topic.

```bash
java -jar trade-producer/target/trade-producer-1.0-SNAPSHOT.jar <bootstrap servers> <trades per sec>
```

#### Start the Jet cluster

Start a jet cluster by running embedded jet server:

```bash
java -jar jet-server/target/jet-server-1.0-SNAPSHOT.jar
```

To configure cluster member you can edit `hazelcast.yaml` in the
`jet-server/src/main/resources` folder. You can refer to the file
`config/examples/hazelcast-full-example.yaml` as it contains all
configuration keys and their descriptions for the Hazelcast cluster.

#### Run The Queries

The cluster connection can be configured inside the
`hazelcast-client.yaml` file. You can specify configurations such as the
cluster address in it. You can refer to the file
`config/examples/hazelcast-client-full-example.yaml` that includes a
descriptions of all configuration options.

* Load static data into map: (Stock names)

```bash
java -jar trade-queries/target/trade-queries-1.0-SNAPSHOT.jar load-symbols
```

* Ingest trades queries from Kafka

```bash
java -jar trade-queries/target/trade-queries-1.0-SNAPSHOT.jar ingest-trades <bootstrap servers>
```

* Aggregate trades by symbol

```bash
java -jar trade-queries/target/trade-queries-1.0-SNAPSHOT.jar aggregate-query <bootstrap servers>
```

#### Start The Front-End Application

To start the front-end application run the following:

```bash
java -jar webapp/target/webapp-1.0-SNAPSHOT.jar
```

By default, it starts on port 9000.
Browse to localhost:9000 to see the dashboard.
