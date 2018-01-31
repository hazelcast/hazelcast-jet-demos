# Hazelcast Jet Demo Applications
These are Demonstration applications using Hazelcast Jet. Each is a full application and demonstrates how you can use Jet to solve real-world problems.

For smaller, feature specific samples see https://github.com/hazelcast/hazelcast-jet-code-samples

## Application Demos

* [Real-time Image Recognition](./realtime-image-recognition) - Recognizes images present in the webcam video input with a model trained with CIFAR-10 dataset.

* [Cryptocurrency Realtime Trend](./cryptocurrency-realtime-trend) - Twitter and Reddit content is analyzed in real time to calculate cryptocurrency trend list with popularity index.                                                                   

* [Online Training Traffic Predictor](./online-training-traffic-predictor) - Continuously computes linear regression models from current traffic. Uses the trend from week ago to predict traffic now.

* [Jet Leopard](./jetleopard) - This is a simple example of a sports book and is a good introduction to the Pipeline API. It also uses Hazelcast IMDG as an in-memory data store.

* [Flight Telemetry](./flight-telemetry) - Reads a stream of telemetry data from ADB-S on all commercial aircraft flying anywhere in the world. There is typically 5,000 - 6,000 aircraft at any point in time. This is then filtered, aggregated and certain features are enriched and displayed in Grafana.

* [Markov Chain Generator](./markov-chain-generator) Generates a Markov Chain with probabilities based on supplied classical books.

* [Customer Churn Predictor](./customer-churn-predictor)

* [Market Data Distributor](#)

## External Demos

* [Real-Time Trade Processing](https://github.com/oliversalmon/imcs-demo) Oliver Buckley-Salmon. Reads from a Kafka topic with Jet and then storage to HBase and Hazelcast IMDG. Shows enrichment and streaming aggregations. Jet 0.4. 

## Prerequisites

- Git Large File Storage : [Installation Guide](https://git-lfs.github.com/)
  Some of the demo applications includes machine learning models in their use cases. Since some models' size exceeds 
  GitHub's 100MB file storage limit this repository uses Git LFS.
- Java Development Kit 8+ : [Installation Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)
- Apache Maven : [Installation Guide](https://maven.apache.org/install.html)
