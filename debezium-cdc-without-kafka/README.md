# Hazelcast Jet Change Data Capture Demo

In this code sample we will show how would you implement a Change Data Capture scenario
with MySQL and a Hazelcast Jet cluster inside Docker environment with Docker Compose.

The Docker Compose file (`hazelcast.yml`) contains following services:

- `mysql` - This image starts a MySQL server with example data and necessary 
configuration for Debezium.
- `hazelcast-jet` - This image starts plain Hazelcast Jet Member.
- `hazelcast-jet-submit` - This image submits a Jet job that was packaged as a
 self-contained JAR file to the Hazelcast Jet Cluster via CLI.

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

This will start the services and the job. The job will listen for changes on the
configured `inventory` database and logs the events as they arrive to the 
standard out. The source for the job can be found [here](https://github.com/eminn/cdc-demo/blob/master/src/main/java/CDCWithJet.java)


# Viewing the Logs

After running the application, see the Hazelcast Jet logs with:

```
make tailServer
```    

It should print logs similar to below after printing huge amount of logs 
regarding initial database snapshot:

``` 
....
hazelcast-jet_1         | 2020-01-07 16:26:31,106  INFO [Threads] [hz.elated_clarke.jet.blocking.thread-0] - Creating thread debezium-mysqlconnector-dbserver1-binlog-client
hazelcast-jet_1         | 2020-01-07 16:26:31,114  INFO [Threads] [blc-mysql:3306] - Creating thread debezium-mysqlconnector-dbserver1-binlog-client
hazelcast-jet_1         | [2020-01-07 16:26:31.269] [INFO   ] com.github.shyiko.mysql.binlog.BinaryLogClient connect - Connected to mysql:3306 at mysql-bin.000003/154 (sid:184054, cid:4)  
hazelcast-jet_1         | 2020-01-07 16:26:31,272  INFO [BinlogReader] [blc-mysql:3306] - Connected to MySQL binlog at mysql:3306, starting at binlog file 'mysql-bin.000003', pos=154, skipping 0 events plus 0 rows
hazelcast-jet_1         | 2020-01-07 16:26:31,274  INFO [Threads] [blc-mysql:3306] - Creating thread debezium-mysqlconnector-dbserver1-binlog-client
```

Optionally if you would like to see the logs for other services use the following: 

- For MySQL Database

    ```
    make tailDb
    ```

- For Hazelast Jet Bootstrap Application

    ```
    make tailClient
    ```

# Inserting data to MySQL
We will insert a new row to one of the tables that we are listening and see that
a new event will be propagated by the Debezium source and logged in the Jet pipeline.

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
hazelcast-jet_1 | 2020-01-07 16:32:19,871  INFO [loggerSink#0] [hz.elated_clarke.jet.blocking.thread-1] - [172.30.0.3]:5701 [cdc-demo] [4.0-SNAPSHOT] 
({"id":1004}, 
{
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
    "ts_ms": 1578414739000,
    "snapshot": "false",
    "db": "inventory",
    "table": "customers",
    "server_id": 223344,
    "gtid": null,
    "file": "mysql-bin.000003",
    "pos": 364,
    "row": 0,
    "thread": 5,
    "query": null
  },
  "op": "u",
  "ts_ms": 1578414739610
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
hazelcast-jet_1 | 2020-01-07 16:38:30,114  INFO [loggerSink#0] [hz.elated_clarke.jet.blocking.thread-1] - [172.30.0.3]:5701 [cdc-demo] [4.0-SNAPSHOT] 
({"id":1005}, 
{
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
    "ts_ms": 1578415109000,
    "snapshot": "false",
    "db": "inventory",
    "table": "customers",
    "server_id": 223344,
    "gtid": null,
    "file": "mysql-bin.000003",
    "pos": 725,
    "row": 0,
    "thread": 5,
    "query": null
  },
  "op": "c",
  "ts_ms": 1578415109778
}
)

```

# Killing the Application

After you are done with the cluster, you can kill it with:

```bash
make down
```





