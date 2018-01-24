# jetleopard

A simple distributed sports book example, to demonstrate Hazlecast Jet.

This application is a "sequel" to betleopard, which previously showed the same
ideas as expressed in Java 8 streams and Apache Spark (accessed via a Hazelcast
API and bridge).

To make sense of jetleopard, users should already be familiar with betleopard and
the domain model that was introduced there.

## Building JetLeopard

Jet Leopard is a variation on Bet Leopard. You must have built BetLeopard and installed it
in your local Maven repository. 

```text
git clone https://github.com/hazelcast/betleopard/
cd betleopard
mvn clean install
```

Then you can build JetLeopard.


## The JetLeopard White Paper

Included in this demo is a [white paper](./JetLeopardWhitePaper.md).

Full details are in the whitepaper, which is in the Asciidoc file jetleopard-wp.adoc 

## Code layout

The package structure is very straightforward:

* com.jetleopard - The jetleopard core classes

## To generate the docs

Documentation is included in the distribution, but to regenerate the docs, do 
the following. From the src/main/java directory, run this:

----
javadoc -D ../../../doc -subpackages com.jetleopard com.jetleopard
----
