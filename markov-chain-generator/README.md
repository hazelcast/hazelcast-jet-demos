# Markov Chain Generator
Markov Chain is a stochastic model describing a sequence of possible events in which the probability of each event depends only on the state attained in the previous event.

This application calculates word transition probabilities from the classical books and stores those in a Hazelcast IMap.

Then they are used to create sentences of specified length.

To learn more about Markov Chains, see: [Wikipedia Article](https://en.wikipedia.org/wiki/Markov_chain)

# Building the Application

To build and package the application, run :

```bash
mvn clean package
```

# Running the Application

After building the application, run it with : 

```bash
mvn exec:java -Dexec.mainClass="MarkovChainGenerator" 
```


