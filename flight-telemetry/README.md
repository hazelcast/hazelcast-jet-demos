# ADB-S Flight Telemetry Stream Processing Demo

Reads a ADB-S telemetry stream from [ADB-S Exchange](https://www.adsbexchange.com/) on all commercial aircraft flying anywhere in the world. 
There is typically 5,000 - 6,000 aircraft at any point in time. 
This is then filtered, aggregated and certain features are enriched and displayed in Grafana
 service provides real-time information about flights. 
             

The demo will calculate following metrics and publish them in Grafana
- Filter out planes outside of defined airports
- Sliding over last 1 minute to detect, whether the plane is ascending, descending or staying in the same level 
- Based on the plane type and phase of the flight provides information about maximum noise levels nearby to the airport and estimated C02 emissions for a region


#Prerequisites

This demo application will output it's results to a Graphite database for visualization with Grafana.

You can easily start a Graphite and Grafana setup from the following repository.

**Please follow the instructions on the [Grafana and Graphite Docker Image](https://github.com/eminn/docker-grafana-graphite) 
repository to run Grafana and Graphite then run this application.**

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
java -jar jet-demo-1.0-SNAPSHOT-jar-with-dependencies.jar
```

