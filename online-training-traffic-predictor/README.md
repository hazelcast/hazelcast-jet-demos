## Online Training with Jet - Traffic predictor

Continuously computes linear regression models from current traffic. Uses
the trend from week ago to predict traffic now.

The demo takes data from a file and writes back to another file, real-life
job can take data from streaming source, such as Kafka.

Sample data extracted from [here](https://catalog.data.gov/dataset/nys-thruway-origin-and-destination-points-for-all-vehicles-15-minute-intervals-2016-q1). 

# Building the Application

To build and package the application, run:

```bash
mvn clean package
```

# Running the Application

Then run the application with: 
```bash
mvn exec:java -Dexec.mainClass="Main"
```