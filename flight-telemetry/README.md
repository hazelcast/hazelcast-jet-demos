# ADB-S Flight Telemetry Stream Processing Demo

Reads a ADB-S telemetry stream from [ADB-S Exchange](https://www.adsbexchange.com/) on all commercial aircraft flying anywhere in the world.
There is typically 5,000 - 6,000 aircraft at any point in time.
This is then filtered, aggregated and certain features are enriched and displayed in Grafana
 service provides real-time information about flights.


The demo will calculate following metrics and publish them in Grafana
- Filter out planes outside of defined airports
- Sliding over last 1 minute to detect, whether the plane is ascending, descending or staying in the same level 
- Based on the plane type and phase of the flight provides information about maximum noise levels nearby to the airport and estimated C02 emissions for a region

## Package Level Structure
At the highest level, the source code is organized into three packages

- com.hazelcast.jet.demo       The main class (`FlightTelemetry`) with the main method and the domain model classes resides in this package.
- com.hazelcast.jet.demo.types The enum classes reside in this package.
- com.hazelcast.jet.demo.util  The utility helper classes reside in this package.


# Prerequisites

- Docker with Docker Compose: [Installation guide](https://docs.docker.com/install/)

Docker must be running for this demo to work. 

This demo application will output it's results to a Graphite database for visualization with Grafana.

You can easily start a Graphite and Grafana setup with Docker with the provided `docker-compose.yml` and  `Makefile` script.

You use following command to run the Graphite and Grafana Docker image:

```bash
$ make up
```

When you are done with the demo, to stop the container, run the following command:
```bash
$ make down
```

In case you need to log into running docker container shell, run the following command:
```bash
$ make shell
```

In case you need to view the container log , run the following command:
```bash
$ make tail
```

# Building the Application

To build and package the application, run:

```bash
mvn clean package
```

# Running the Application

After building the application, run the application with:

```bash
mvn exec:java
```

Then navigate to with your browser `localhost` to open up the Grafana application.
To log into Grafana use `admin/admin` username/password pair. 
You need to select `Flight Telemetry` dashboard to see the metrics 
that are emitted from the Flight Telemetry application.

Note: The ADB-S data stream publishes ~3 MB of data per update. We are polling it every 10 seconds by default, so you might need a decent internet connection for demo to work properly. Otherwise you might see some delay on the charts/output.

## Fault Tolerant Mode

This demo has two modes. The default mode, as described above, works with `mvn exec:java` and is not fault tolerant. 
The second mode is fault tolerant and offers exactly-once processing guarantee. In this mode, you can start multiple 
Jet nodes and form a Jet cluster. In addition, you can start new nodes while the job is running and scale out the job,
or you can kill some of the nodes. To start a Jet node, run: 

```bash
mvn exec:java -Pft -Dtype=server
```

After the Jet node is started, the job is not automatically submitted. Instead, a command line console starts. 
In one of the consoles that start with the Jet nodes, type `submit` to submit the job. Then, in other nodes, 
you can type `get` to get a reference to the submitted job. After this point, you can track and manage the 
running job in any Jet node. You can type `help` to see the available commands.     

This mode replaces the source processor with a reliable map journal. Instead of polling the aircraft events directly 
from the external API, the job uses a map journal as its source. For this reason, we have another process to poll the 
data from the external API and populate the source map. To start the source process, run:
```bash
mvn exec:java -Pft -Dtype=source
```

Please see `com.hazelcast.jet.demo.FaultTolerantFlightTelemetry` for more information.
