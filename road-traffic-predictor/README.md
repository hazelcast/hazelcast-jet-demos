# Online Training with Jet - Traffic predictor

This demo shows how to use Jet for real-time machine learning use-case.
It combines real-time model training and prediction into one Jet
Pipeline.

Jet reads the traffic data. Each input record contains timestamp,
location and respective car count. After ingestion, the records are
routed to two separate computations.

First computation uses the car count to train the model. Jet collects
data from last two hours and computes the trend using the [linear
regression](https://en.wikipedia.org/wiki/Linear_regression) algorithm.
This trend is updated every 15 minutes and stored into an IMap
(distributed map implementation available in Jet). [Sliding
Windows](http://docs.hazelcast.org/docs/jet/0.5.1/manual/Work_with_Jet/Infinite_Stream_Processing.html#page_Windowing)
are used to select two hour blocks from the time series.

Second computation combines current car count with the trend from
previous week to predict car count in next two hours. Prediction results
are stored in a file in `predictions/` directory.

For example, a record `2016-03-23T18:15,57A,23` in the input data set
indicates that the car count was 23 at the intersection with ID 57A on
23rd March 2016, 6:45 PM. This information is used to compute the trend.
Prediction for the same record says that at 7:00 PM, expected car count
is 38. At 7:15 it's 36 etc.

```
Prediction{location='57A', time=2016-03-23T18:15 (1458753300000), counts=[38, 36, 34, 32, 30, 28, 26, 24]}
```

For the sake of simplicity, this demo reads the traffic data from the
[CSV
file](https://github.com/hazelcast/hazelcast-jet-demos/blob/master/online-training-traffic-predictor/15-minute-counts-sorted.csv).
However, one can easily attach this pipeline to a real streaming source,
such as Apache Kafka.

Sample data were extracted from
[catalog.data.gov](https://catalog.data.gov/dataset/nys-thruway-origin-and-destination-points-for-all-vehicles-15-minute-intervals-2016-q1).
 

## Package Level Structure

The main class (`TrafficPredictor`) with the main method resides in the
default package.

# Building the Application

To build and package the application, run:

```bash
mvn clean package
```

# Running the Application

Then run the application with: 

```bash
mvn exec:java
```
