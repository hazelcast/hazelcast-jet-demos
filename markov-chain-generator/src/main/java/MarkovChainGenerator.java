/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.hazelcast.jet.IMapJet;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.core.AppendableTraverser;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hazelcast.jet.aggregate.AggregateOperations.allOf;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.aggregate.AggregateOperations.groupingBy;
import static com.hazelcast.jet.datamodel.Tuple2.tuple2;

/**
 * This application calculates word transition probabilities
 * from the classical books and stores those in a Hazelcast IMap.
 * Then they are used to create markov chains of specified length.
 *
 * The DAG used to model markov chain calculation can be seen below :
 *
 *               ┌─────────────────────────┐
 *               │Read books from directory│
 *               └─────────────┬───────────┘
 *                             │
 *                             v
 *                ┌────────────────────────┐
 *                │  FlatMap the book in   │
 *                │consecutive 2-word pairs│
 *                └────────────┬───────────┘
 *                             │
 *                             v
 *              ┌────────────────────────────┐
 *              │Group by with the first word│
 *              └──────────────┬─────────────┘
 *                             │
 *                             v
 *         ┌──────────────────────────────────────┐
 *         │Calculate probability map for the word│
 *         └───────────────────┬──────────────────┘
 *                             │
 *                             v
 *         ┌───────────────────────────────────────┐
 *         │Write results to IMap(stateTransitions)│
 *         └───────────────────────────────────────┘
 *
 */
public class MarkovChainGenerator {

    private static final String INPUT_FILE = MarkovChainGenerator.class.getResource("books").getPath();
    private static final Random RANDOM = new Random();

    static {
        System.setProperty("hazelcast.logging.type", "log4j");
    }

    public static void main(String[] args) {
        JetInstance jet = Jet.newJetInstance();
        Pipeline p = buildPipeline();

        System.out.println("Generating model...");
        try {
            jet.newJob(p).join();
            printTransitionsAndMarkovChain(jet);
        } finally {
            Jet.shutdownAll();
        }
    }

    /**
     * Builds and returns the Pipeline which represents the actual computation.
     * To compute the probability of finding word B after A, one has to know
     * how many pairs contain word A as a first entry and how many of them
     * contain B as a second entry. The pipeline creates pairs from consecutive
     * words and computes the probabilities of A->B.
     */
    private static Pipeline buildPipeline() {
        Pipeline p = Pipeline.create();
        // Reads files line-by-line
        BatchStage<String> lines = p.drawFrom(Sources.<String>files(INPUT_FILE));
        Pattern twoWords = Pattern.compile("(\\.|\\w+)\\s(\\.|\\w+)");
        // Calculates probabilities by flatmapping lines into two-word consecutive pairs using regular expressions
        // and aggregates them into an IMap.
        lines.flatMap(e -> traverseMatcher(twoWords.matcher(e.toLowerCase()), m -> tuple2(m.group(1), m.group(2))))
             .groupingKey(Tuple2::f0)
             .aggregate(buildAggregateOp())
             .drainTo(Sinks.map("stateTransitions"));
        return p;
    }

    /**
     * Prints state transitions from IMap, generates the markov chain and prints it
     */
    private static void printTransitionsAndMarkovChain(JetInstance jet) {
        IMapJet<String, SortedMap<Double, String>> transitions = jet.getMap("stateTransitions");
        printTransitions(transitions);
        String chain = generateMarkovChain(1000, transitions);
        System.out.println(chain);
    }


    /**
     * Generates a markov chain of specified length
     *
     * @param length      length of the markov chain
     * @param transitions transitions map for the words
     */
    private static String generateMarkovChain(int length, Map<String, SortedMap<Double, String>> transitions) {
        StringBuilder builder = new StringBuilder();
        String word = nextWord(transitions.get("."));
        builder.append(capitalizeFirst(word));
        for (int i = 0; i < length; i++) {
            SortedMap<Double, String> t = transitions.get(word);
            if (t == null) {
                word = nextWord(transitions.get("."));
                builder.append(". ").append(capitalizeFirst(word));
            } else {
                word = nextWord(t);
                if (word.equals(".")) {
                    builder.append(word);
                } else {
                    builder.append(" ").append(word);
                }
            }
        }
        return builder.toString();
    }
    /**
     * Returns the next word from the transitions table.
     */
    private static String nextWord(SortedMap<Double, String> transitions) {
        return transitions.tailMap(RANDOM.nextDouble()).values().iterator().next();
    }

    /**
     * Capitalizes the first character on the word
     */
    private static String capitalizeFirst(String word) {
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    /**
     * Creates an aggregation operation for calculating probabilities for a word
     */
    private static AggregateOperation1<Tuple2<String, String>, ?, SortedMap<Double, String>> buildAggregateOp() {
        return allOf(
                counting(),
                groupingBy(Tuple2::f1, counting()),
                (totals, counts) -> {
                    SortedMap<Double, String> probabilities = new TreeMap<>();
                    double cumulative = 0.0;
                    for (Entry<String, Long> e : counts.entrySet()) {
                        cumulative += e.getValue() / (double) totals;
                        probabilities.put(cumulative, e.getKey());
                    }
                    return probabilities;
                }
        );
    }

    /**
     * Prints the transition table for 10 entries.
     */
    private static void printTransitions(Map<String, SortedMap<Double, String>> counts) {
        counts.entrySet().stream().limit(10).forEach(e -> printTransitions(e.getKey(), e.getValue()));
    }

    /**
     * Prints the transition table for the supplied word
     */
    private static void printTransitions(String key, SortedMap<Double, String> probabilities) {
        System.out.println("Transitions for: " + key);
        System.out.println("/-------------+-------------\\");
        System.out.println("| Probability | Word        |");
        System.out.println("|-------------+-------------|");
        probabilities.forEach((p, w) -> System.out.format("|  %.4f     | %-12s|%n", p, w));
        System.out.println("\\-------------+-------------/");
    }

    /**
     * Creates and returns a traverser which traverses matched items on the supplied matcher.
     * Also applies the supplied mapping function to the matched items.
     */
    private static <R> Traverser<R> traverseMatcher(Matcher m, Function<Matcher, R> mapperFn) {
        AppendableTraverser<R> traverser = new AppendableTraverser<>(1);
        while (m.find()) {
            traverser.append(mapperFn.apply(m));
        }
        return traverser;
    }
}
