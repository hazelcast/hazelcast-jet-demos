# Cryptocurrency Realtime Trend

Twitter content is analyzed in real time to calculate cryptocurrency trend list with its popularity index.

This demo shows how to ingest a Tweet stream and how to use streaming operations in Jet (windowing, event-time processing, streaming aggregations). It also demonstrates integrating of 3rd party NLP library into the parallel data pipeline.      

# Data Pipeline

![](./diagram.png)

The tweets are read from Twitter and categorized by coin type (BTC, ETC, XRP, etc). In next step, NLP sentimental analysis is applied to each tweet to calculate the sentiment score of the respective tweet. This score says whether the Tweet has rather positive or negative sentiment. Jet uses Stanford NLP lib to compute it.

 For each cryptocurrency, Jet aggregates scores from last 30 seconds, last minute and last 5 minutes and prints the coin popularity table to the output like below: 
 
![](./output.png)
  

## Prerequisites

You'll need to have API Credentials from Twitter to make this demo work.

To obtain them, visit the following website:
- [Twitter Application Management](http://apps.twitter.com/)

Please fill in the Twitter credentials into the file below.

`src/main/resources/twitter-security.properties`


## Building the Application

To build and package the application, run:

>Please note that maven may take some time to download all dependencies on the first run since the NLP libraries are hefty in file size.

```bash
mvn clean package
```

## Running the Application

After building the application, run the application with: 

```bash
mvn exec:java
```
