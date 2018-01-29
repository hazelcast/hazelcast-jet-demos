# ADB-S Flight Telemetry Stream Processing Demo

Reads a ADB-S telemetry stream from [ADB-S Exchange](https://www.adsbexchange.com/) on all commercial aircraft flying anywhere in the world. 
There is typically 5,000 - 6,000 aircraft at any point in time. 
This is then filtered, aggregated and certain features are enriched and displayed in Grafana
 service provides real-time information about flights. 
             

The demo will calculate following metrics and publish them in Grafana
- Filter out planes outside of defined airports
- Sliding over last 1 minute to detect, whether the plane is ascending, descending or staying in the same level 
- Based on the plane type and phase of the flight provides information about maximum noise levels nearby to the airport and estimated C02 emissions for a region


# Prerequisites

- Docker with Docker Compose : [Installation guide](https://docs.docker.com/install/)
- JDK 8+ : [Installation guide](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)

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

To build and package the application, run :

```bash
mvn clean package
```

# Running the Application

After building the application, navigate to `target` folder by :
```bash
cd target
```

Then run the application with : 
```bash
java -jar flight-telemetry-0.1-SNAPSHOT-jar-with-dependencies.jar
```

Then navigate to with your browser `localhost` to open up the Grafana application.
To log into Grafana use `admin/admin` username/password pair. 
You need to select `Flight Telemetry` dashboard to see the metrics 
that are emitted from the Flight Telemetry application.

