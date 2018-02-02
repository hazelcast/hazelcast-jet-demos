# Markov Chain Generator
Markov Chain is a stochastic model describing a sequence of possible events in which the probability of each event depends only on the state attained in the previous event.

This application calculates word transition probabilities from the classical books and stores those in a Hazelcast IMap.

Then they are used to create sentences of specified length.

To learn more about Markov Chains, see: [Wikipedia Article](https://en.wikipedia.org/wiki/Markov_chain)

# Data Pipeline

There is almost 100 MB of classical literature on the input, see [the resources](https://github.com/hazelcast/hazelcast-jet-demos/tree/master/markov-chain-generator/src/main/resources). Jet pipeline builds the Markov Chain based on this data set. The Markov Chain is then used to generate random sentences such as 

```Very tall masts went on any kind for doing that they transmit by an oversight.```

As you can see, the sentence is meaningless. It's structure is however similar to an English sentence as the Markov chain was "trained" on real English texts.
 
Jet makes the computation parallel.

In the first step, Jet reads the books stored in text files and converts single words to pairs. So for `The Picture of Dorian Gray` we get following pairs representing the word to word transitions:

```
(The, Picture), (Picture, of), (of, Dorian), (Dorian, Gray)
```

To compute the probability of finding word B after A, we have to know how many pairs contain word A as a fist entry and how many of them contain B as a second entry. When we compute this for all (A,B) pairs appearing in the source text we get the transition matrix representing the Markov Chain.  

The aggregation features of Jet are used to do the heavy lifting. Jet runs the computation in parallel to make use of all available processor cores.

The computed Markov Chain is stored to an `IMap` (distributed map implementation available in Jet) and printed to standard output. See the probabilities for the word `organizations`:   

```
Transitions for: organizations
/-------------+-------------\
| Probability | Word        |
|-------------+-------------|
|  0,2500     | with        |
|  0,5000     | can         |
|  0,7500     | so          |
|  1,0000     | did         |
\-------------+-------------/
```

This Markov Chain is then used to generate a sentence. Starting from `.` word, next word is iteratively selected based on the probability of appearing after a previous one. The sentence is printed to standard output.


# Building the Application

To build and package the application, run:

```bash
mvn clean package
```

# Running the Application

After building the application, run it with: 

```bash
mvn exec:java 
```


