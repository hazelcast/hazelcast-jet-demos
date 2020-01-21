# Hazelcast Jet Change Data Capture Demo

In this code sample we will show how would you implement a Change Data Capture scenario
with Debezium, Kafka, MySQL and a Hazelcast Jet cluster inside Docker environment with Docker Compose.

The Docker Compose file (`cdc.yml`) contains following services:

- `mysql` - This service starts a MySQL server with example data and necessary 
configuration for Debezium.
- `hazelcast-jet` - This service starts plain Hazelcast Jet Member.
- `hazelcast-jet-submit` - This service submits a Jet job that was packaged as a
 self-contained JAR file to the Hazelcast Jet Cluster via CLI.
- `zoo` - This service starts Zookeeper instance required for the Kafka broker.  
- `kafka` - This service starts Kafka broker used to store change data  
- `kafka-connect` - This service starts Kafka Connect component to run Debezium 
Connector which has it pre-installed.  


# Prerequisites

- Docker with Docker Compose: [Installation guide](https://docs.docker.com/install/)

Docker service/daemon must be running for this application to work.


# Building the Application

To build and package the application, run:

```bash
mvn clean package
```

# Running the Application

After building the application, run the application with:

```bash
make up
```

This will start the services and the Hazelcast Jet pipeline. At this stage we have
MySQL, Kafka, Kafka Connect, ZooKeeper and Hazelcast Jet is running. The Kafka
Connect service has not started the Debezium MySQL connector yet. 

So we'll start the Debezium MySQL connector with the following command:

```bash
make startDebezium
```

It'll start the Debezium MySQL connector with the following properties:

```json 
> cat mysql-connector.json
{
  "name": "inventory-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "tasks.max": "1",
    "database.hostname": "mysql",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "dbz",
    "database.server.id": "184054",
    "database.server.name": "dbserver1",
    "database.whitelist": "inventory",
    "database.history.kafka.bootstrap.servers": "kafka:9092",
    "database.history.kafka.topic": "schema-changes.inventory"
  }
}
```  

After running the Debezium MySQL connector, we can check the Kafka Connect logs with:

```
make tailKafkaConnect
```    

It should print logs similar to below after printing huge amount of logs 
regarding initial database snapshot:

``` 
....
kafka-connect_1 | 2020-01-08 13:52:18,018 INFO   MySQL|dbserver1|task  Creating thread debezium-mysqlconnector-dbserver1-binlog-client   [io.debezium.util.Threads]
kafka-connect_1 | 2020-01-08 13:52:18,027 INFO   MySQL|dbserver1|task  Creating thread debezium-mysqlconnector-dbserver1-binlog-client   [io.debezium.util.Threads]
kafka-connect_1 | Jan 08, 2020 1:52:18 PM com.github.shyiko.mysql.binlog.BinaryLogClient connect
kafka-connect_1 | INFO: Connected to mysql:3306 at mysql-bin.000003/154 (sid:184054, cid:6)
kafka-connect_1 | 2020-01-08 13:52:18,218 INFO   MySQL|dbserver1|binlog  Connected to MySQL binlog at mysql:3306, starting at binlog file 'mysql-bin.000003', pos=154, skipping 0 events plus 0 rows   [io.debezium.connector.mysql.BinlogReader]
kafka-connect_1 | 2020-01-08 13:52:18,220 INFO   MySQL|dbserver1|binlog  Creating thread debezium-mysqlconnector-dbserver1-binlog-client   [io.debezium.util.Threads]
```
      
Since initial snapshot has been completed, we can inpsect the Hazelcast Jet logs with:

```
make tailServer
```    

The running Hazelcast Jet pipeline listen for changes on the Kafka topic 
named `dbserver1.inventory.customers`, logs the events as they arrive to the 
standard out and puts them to an IMap. 

The source for the Jet pipeline can be found [here](https://github.com/eminn/cdc-demo/blob/kafka-connect/src/main/java/com/hazelcast/jet/demo/cdc/CDCWithJet.java)

So it should print logs similar to below (schema definition in the events are omitted and 
JSON is pretty printed for clarity ):

```
hazelcast-jet_1 | 2020-01-08 13:52:16,594  INFO [streamKafka(dbserver1_inventory_customers)#0] [hz.sweet_wu.jet.blocking.thread-0] - [172.23.0.6]:5701 [cdc-demo] [4.0-SNAPSHOT] Output to ordinal 0: 
({
  "payload": {
    "id": 1001
  }
},{
  "payload": {
    "before": null,
    "after": {
      "id": 1001,
      "first_name": "Sally",
      "last_name": "Thomas",
      "email": "sally.thomas@acme.com"
    },
    "source": {
      "version": "1.0.0.Final",
      "connector": "mysql",
      "name": "dbserver1",
      "ts_ms": 0,
      "snapshot": "true",
      "db": "inventory",
      "table": "customers",
      "server_id": 0,
      "gtid": null,
      "file": "mysql-bin.000003",
      "pos": 154,
      "row": 0,
      "thread": null,
      "query": null
    },
    "op": "c",
    "ts_ms": 1578491535844
  }
})


```  

You can also check the original events in the Kafka Topic named `dbserver1.inventory.customers`
with the command below:

```
> make tailKafkaCustomersTopic
Using ZOOKEEPER_CONNECT=zoo:2181
Using KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://172.23.0.4:9092
Using KAFKA_BROKER=kafka:9092
Contents of topic dbserver1.inventory.customers:
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbserver1.inventory.customers.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbserver1.inventory.customers.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":false,"field":"version"},{"type":"string","optional":false,"field":"connector"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"ts_ms"},{"type":"string","optional":true,"name":"io.debezium.data.Enum","version":1,"parameters":{"allowed":"true,last,false"},"default":"false","field":"snapshot"},{"type":"string","optional":false,"field":"db"},{"type":"string","optional":true,"field":"table"},{"type":"int64","optional":false,"field":"server_id"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"query"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.inventory.customers.Envelope"},"payload":{"before":null,"after":{"id":1001,"first_name":"Sally","last_name":"Thomas","email":"sally.thomas@acme.com"},"source":{"version":"1.0.0.Final","connector":"mysql","name":"dbserver1","ts_ms":0,"snapshot":"true","db":"inventory","table":"customers","server_id":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"thread":null,"query":null},"op":"c","ts_ms":1578491535844}}
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbserver1.inventory.customers.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbserver1.inventory.customers.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":false,"field":"version"},{"type":"string","optional":false,"field":"connector"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"ts_ms"},{"type":"string","optional":true,"name":"io.debezium.data.Enum","version":1,"parameters":{"allowed":"true,last,false"},"default":"false","field":"snapshot"},{"type":"string","optional":false,"field":"db"},{"type":"string","optional":true,"field":"table"},{"type":"int64","optional":false,"field":"server_id"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"query"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.inventory.customers.Envelope"},"payload":{"before":null,"after":{"id":1002,"first_name":"George","last_name":"Bailey","email":"gbailey@foobar.com"},"source":{"version":"1.0.0.Final","connector":"mysql","name":"dbserver1","ts_ms":0,"snapshot":"true","db":"inventory","table":"customers","server_id":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"thread":null,"query":null},"op":"c","ts_ms":1578491535844}}
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbserver1.inventory.customers.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbserver1.inventory.customers.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":false,"field":"version"},{"type":"string","optional":false,"field":"connector"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"ts_ms"},{"type":"string","optional":true,"name":"io.debezium.data.Enum","version":1,"parameters":{"allowed":"true,last,false"},"default":"false","field":"snapshot"},{"type":"string","optional":false,"field":"db"},{"type":"string","optional":true,"field":"table"},{"type":"int64","optional":false,"field":"server_id"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"query"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.inventory.customers.Envelope"},"payload":{"before":null,"after":{"id":1003,"first_name":"Edward","last_name":"Walker","email":"ed@walker.com"},"source":{"version":"1.0.0.Final","connector":"mysql","name":"dbserver1","ts_ms":0,"snapshot":"true","db":"inventory","table":"customers","server_id":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"thread":null,"query":null},"op":"c","ts_ms":1578491535844}}
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbserver1.inventory.customers.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbserver1.inventory.customers.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":false,"field":"version"},{"type":"string","optional":false,"field":"connector"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"ts_ms"},{"type":"string","optional":true,"name":"io.debezium.data.Enum","version":1,"parameters":{"allowed":"true,last,false"},"default":"false","field":"snapshot"},{"type":"string","optional":false,"field":"db"},{"type":"string","optional":true,"field":"table"},{"type":"int64","optional":false,"field":"server_id"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"query"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.inventory.customers.Envelope"},"payload":{"before":null,"after":{"id":1004,"first_name":"Anne","last_name":"Kretchmar","email":"annek@noanswer.org"},"source":{"version":"1.0.0.Final","connector":"mysql","name":"dbserver1","ts_ms":0,"snapshot":"true","db":"inventory","table":"customers","server_id":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"thread":null,"query":null},"op":"c","ts_ms":1578491535844}}
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbserver1.inventory.customers.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbserver1.inventory.customers.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":false,"field":"version"},{"type":"string","optional":false,"field":"connector"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"ts_ms"},{"type":"string","optional":true,"name":"io.debezium.data.Enum","version":1,"parameters":{"allowed":"true,last,false"},"default":"false","field":"snapshot"},{"type":"string","optional":false,"field":"db"},{"type":"string","optional":true,"field":"table"},{"type":"int64","optional":false,"field":"server_id"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"query"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.inventory.customers.Envelope"},"payload":{"before":{"id":1004,"first_name":"Anne","last_name":"Kretchmar","email":"annek@noanswer.org"},"after":{"id":1004,"first_name":"Anne Marie","last_name":"Kretchmar","email":"annek@noanswer.org"},"source":{"version":"1.0.0.Final","connector":"mysql","name":"dbserver1","ts_ms":1578491545000,"snapshot":"false","db":"inventory","table":"customers","server_id":223344,"gtid":null,"file":"mysql-bin.000003","pos":364,"row":0,"thread":4,"query":null},"op":"u","ts_ms":1578491545649}}
``` 

Optionally if you would like to see the logs for other services use the following: 

- For MySQL Database

    ```
    make tailDb
    ```           
  
- For Kafka Broker

    ```
    make tailKafka
    ```

- For Kafka Topic named `dbserver1.inventory.customers` 

    ```
    make tailKafkaCustomersTopic
    ```

- For Kafka Connect

    ```
    make tailKafkaConnect
    ```

# Updating or Inserting data to MySQL
We will insert a new row to one of the tables that we are listening to see that
a new event will be propagated by the Debezium to a Kafka topic and logged in 
the Jet pipeline.

To make changes, log into the database by using the command below:

```
make connectDb
```

You should see the mysql command prompt:

```
Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> 
```       

You can run an example query to see the contents of the `customers` table: 

``` 
mysql> select * from customers;
+------+------------+-----------+-----------------------+
| id   | first_name | last_name | email                 |
+------+------------+-----------+-----------------------+
| 1001 | Sally      | Thomas    | sally.thomas@acme.com |
| 1002 | George     | Bailey    | gbailey@foobar.com    |
| 1003 | Edward     | Walker    | ed@walker.com         |
| 1004 | Anne       | Kretchmar | annek@noanswer.org    |
+------+------------+-----------+-----------------------+
4 rows in set (0.01 sec)
``` 

Now, we can update a record in the table with following query:

```                 
mysql> UPDATE customers SET first_name='Anne Marie' WHERE id=1004;
Query OK, 1 row affected (0.01 sec)
Rows matched: 1  Changed: 1  Warnings: 0
```     

Switching back to the Hazelcast Jet logs, we should see the update record like 
following:

```   
make tailServer
...
...
...
hazelcast-jet_1| 2020-01-08 13:52:26,027  INFO [streamKafka(dbserver1_inventory_customers)#0] [hz.sweet_wu.jet.blocking.thread-0] - [172.23.0.6]:5701 [cdc-demo] [4.0-SNAPSHOT] Output to ordinal 0: 
({
   "payload": {
     "id": 1004
   }
 }, 
{
  "payload": {
      "before": {
        "id": 1004,
        "first_name": "Anne",
        "last_name": "Kretchmar",
        "email": "annek@noanswer.org"
      },
      "after": {
        "id": 1004,
        "first_name": "Anne Marie",
        "last_name": "Kretchmar",
        "email": "annek@noanswer.org"
      },
      "source": {
        "version": "1.0.0.Final",
        "connector": "mysql",
        "name": "dbserver1",
        "ts_ms": 1578491545000,
        "snapshot": "false",
        "db": "inventory",
        "table": "customers",
        "server_id": 223344,
        "gtid": null,
        "file": "mysql-bin.000003",
        "pos": 364,
        "row": 0,
        "thread": 4,
        "query": null
      },
      "op": "u",
      "ts_ms": 1578491545649
    }
})
```  

We could also insert a record like following:

```
mysql> INSERT INTO customers (id, first_name, last_name, email) VALUES (1005, 'Jane', 'Doe', 'jane@foo.com');
Query OK, 1 row affected (0.00 sec)
```   

Running the above would yield a log line like below:

``` 
make tailServer
...
...
...
hazelcast-jet_1 | 2020-01-08 15:12:21,374  INFO [streamKafka(dbserver1_inventory_customers)#0] [hz.nostalgic_beaver.jet.blocking.thread-0] - [172.24.0.6]:5701 [cdc-demo] [4.0-SNAPSHOT] Output to ordinal 0:
({
    "payload": {
      "id": 1005
    }
  }, 
{
  "payload": {
      "before": null,
      "after": {
        "id": 1005,
        "first_name": "Jane",
        "last_name": "Doe",
        "email": "jane@foo.com"
      },
      "source": {
        "version": "1.0.0.Final",
        "connector": "mysql",
        "name": "dbserver1",
        "ts_ms": 1578496341000,
        "snapshot": "false",
        "db": "inventory",
        "table": "customers",
        "server_id": 223344,
        "gtid": null,
        "file": "mysql-bin.000003",
        "pos": 364,
        "row": 0,
        "thread": 6,
        "query": null
      },
      "op": "c",
      "ts_ms": 1578496341195
  }
}
)

```

# Killing the Application

After you are done with the cluster, you can kill it with:

```bash
make down
```





