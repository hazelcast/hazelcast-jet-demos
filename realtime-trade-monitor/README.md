# Real-time dto.Trade Monitor

This demo shows how we can monitor real-time trades via using
aggregations of the Jet. A sample dashboard which uses [Hazelcast
Jet](https://github.com/hazelcast/hazelcast-jet) to ingest trades from
Apache Kafka into a distributed map. It also performs an aggregation on
the trades, storing the results into a separate map.

These two maps are utilized by a live dashboard which offers drill down
functionality into viewing individual trades that make up the
aggregation.

## How to run

### Build the project

```bash
mvn package
```

### Prepare The Environment

#### Create a topic with four partitions and only one replica on the Kafka cluster:

```bash
kafka-topics --create --replication-factor 1 --partitions 4 --topic trades --bootstrap-server localhost:9092
```

#### Start the Kafka producer

```bash
java -jar trade-producer/target/trade-producer-1.0-SNAPSHOT.jar <bootstrap servers> <trades per sec>
```

#### Start the Jet cluster

To configure cluster members you can edit `hazelcast.yaml` in the
`jet-server/src/main/resources` folder. You can refer to
[this](https://github.com/hazelcast/hazelcast/blob/master/hazelcast/src/main/resources/hazelcast-full-example.yaml)
as it contains all configuration keys and descriptions for hazelcast
cluster.

```bash
java -jar jet-server/target/jet-server-1.0-SNAPSHOT.jar
```

#### Run the queries

The cluster connection can be configured inside the
`hazelcast-client.yaml` file.
[](https://github.com/hazelcast/hazelcast/blob/master/hazelcast/src/main/resources/hazelcast-client-full-example.yaml)

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

#### Start the front end application

The cluster connection can be configured inside the `hazelcast-client.yaml` file.

```bash
java -jar webapp/target/webapp-1.0-SNAPSHOT.jar
```

Browse to localhost:9000 to see the dashboard.
