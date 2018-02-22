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

# Scaling the Application
If you'd like to scale up the application, you can add a new node to the cluster and restart the application. To achieve
that we've included the `AdditionalMember` class implements the scenario.

You can see the implementation of the `AdditionalMember` class below.

```java
/**
 * It starts a new Jet instance and restart the currently running job to scale it to run on all members
 * including the new launched one.
 */
public class AdditionalMember {
    public static void main(String[] args) {
        System.setProperty("hazelcast.logging.type", "log4j");
        JetInstance jet = Jet.newJetInstance();
        Job job = jet.getJob(FlightTelemetry.JOB_NAME);

        makeSureThatJobExists(job);
        makeSureThatJobIsRunning(job);

        job.restart();
        job.join();
    }

    private static void makeSureThatJobExists(Job job) {
        if (job == null) {
            throw new IllegalStateException("Job(" + FlightTelemetry.JOB_NAME + ") cannot be found. Are you sure that you\'ve started the FlightTelemetry ?");
        }
    }

    private static void makeSureThatJobIsRunning(Job job) {
        while (!JobStatus.RUNNING.equals(job.getStatus())) {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            System.out.println("Waiting for Job(" + FlightTelemetry.JOB_NAME + ")  to be started.");
        }
    }
}

```

You can run the following to scale up the application while `Flight Telemetry` demo is running.

```bash
mvn exec:java -Dexec.mainClass="AdditionalMember"
```
