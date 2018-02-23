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

import com.hazelcast.jet.ComputeStage;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Pipeline;
import com.hazelcast.jet.Sinks;
import com.hazelcast.jet.Sources;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.core.AppendableTraverser;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.stream.IStreamMap;
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

public class MarkovChainGenerator {

    private static final String INPUT_FILE = MarkovChainGenerator.class.getResource("books").getPath();
    private static final Random RANDOM = new Random();

    static {
        System.setProperty("hazelcast.logging.type", "log4j");
    }

    public static void main(String[] args) {
        JetInstance jet = Jet.newJetInstance();
        Pipeline p = createPipeline();

        System.out.println("Generating model...");
        try {
            jet.newJob(p).join();
            printTransitionsAndMarkovChain(jet);
        } finally {
            Jet.shutdownAll();
        }
    }

    private static void printTransitionsAndMarkovChain(JetInstance jet) {
        IStreamMap<String, SortedMap<Double, String>> transitions = jet.getMap("stateTransitions");
        printTransitions(transitions);
        String chain = generateMarkovChain(1000, transitions);
        System.out.println(chain);
    }

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

    private static String nextWord(SortedMap<Double, String> transitions) {
        return transitions.tailMap(RANDOM.nextDouble()).values().iterator().next();
    }

    private static String capitalizeFirst(String word) {
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    private static Pipeline createPipeline() {
        Pipeline p = Pipeline.create();
        ComputeStage<String> lines = p.drawFrom(Sources.<String>files(INPUT_FILE));
        Pattern twoWords = Pattern.compile("(\\.|\\w+)\\s(\\.|\\w+)");
        lines.flatMap(e -> traverseMatcher(twoWords.matcher(e.toLowerCase()), m -> tuple2(m.group(1), m.group(2))))
             .groupBy(Tuple2::f0, buildAggregateOp())
             .drainTo(Sinks.map("stateTransitions"));
        return p;
    }

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

    private static void printTransitions(Map<String, SortedMap<Double, String>> counts) {
        counts.entrySet().stream().limit(10).forEach(e -> printTransitions(e.getKey(), e.getValue()));
    }

    private static void printTransitions(String key, SortedMap<Double, String> probabilities) {
        System.out.println("Transitions for: " + key);
        System.out.println("/-------------+-------------\\");
        System.out.println("| Probability | Word        |");
        System.out.println("|-------------+-------------|");
        probabilities.forEach((p, w) -> System.out.format("|  %.4f     | %-12s|%n", p, w));
        System.out.println("\\-------------+-------------/");
    }

    private static <R> Traverser<R> traverseMatcher(Matcher m, Function<Matcher, R> mapperFn) {
        AppendableTraverser<R> traverser = new AppendableTraverser<>(1);
        while (m.find()) {
            traverser.append(mapperFn.apply(m));
        }
        return traverser;
    }
}
