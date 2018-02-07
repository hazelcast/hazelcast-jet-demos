# jetleopard

A simple distributed sports book example, to demonstrate Hazlecast Jet.

This application is a "sequel" to BetLeopard, which previously showed the same ideas as expressed in Java 8 streams and Apache Spark (accessed via a Hazelcast API and bridge).

To make best use of JetLeopard, users should ideally be familiar with BetLeopard and the domain model that was introduced there.
Basic details are also covered in the white paper.

# Prerequisites

JetLeopard is self-contained, apart from its Maven dependencies.

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

## The JetLeopard White Paper

Included in this demo is a [white paper](./JetLeopardWhitePaper.md).

## Code layout

The package structure is very straightforward:

* com.jetleopard - The jetleopard core classes
* com.betleopard - Shaded core classes from BetLeopard
* com.betleopard.domain - Shaded domain classes from BetLeopard
* com.betleopard.hazelcast - Shaded Hazelcast helper classes from BetLeopard
* com.betleopard.simple - Shaded Java 8 Streams helper classes from BetLeopard

## To generate the docs

Documentation is included in the distribution, but to regenerate the docs, do 
the following. From the src/main/java directory, run this:

```
javadoc -D ../../../doc -subpackages com.jetleopard com.jetleopard
```
