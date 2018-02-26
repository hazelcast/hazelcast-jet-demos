import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.core.AppendableTraverser;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.StreamStage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.aggregate.AggregateOperations.allOf;
import static com.hazelcast.jet.aggregate.AggregateOperations.averagingDouble;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.function.DistributedFunctions.entryKey;
import static com.hazelcast.jet.pipeline.Sinks.map;
import static com.hazelcast.jet.pipeline.WindowDefinition.sliding;
import static com.hazelcast.util.ExceptionUtil.rethrow;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class JetCoinTrend {

    private static final String MAP_NAME_30_SECONDS = "map30Seconds";
    private static final String MAP_NAME_1_MINUTE = "map1Min";
    private static final String MAP_NAME_5_MINUTE = "map5Min";
    private static final long PRINT_INTERNAL_MILLIS = 10_000L;
    private static volatile boolean running = true;

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    static {
        System.setProperty("hazelcast.logging.type", "log4j");
    }

    public static void main(String[] args) {
        System.out.println("DISCLAIMER: This is not an investment advice");

        Pipeline pipeline = buildPipeline();
        // Start Jet
        JetInstance jet = Jet.newJetInstance();
        startConsolePrinterThread(jet);
        try {
            // Perform the computation
            jet.newJob(pipeline).join();
        } finally {
            running = false;
            Jet.shutdownAll();
        }
    }

    private static Pipeline buildPipeline() {
        Pipeline pipeline = Pipeline.create();
        Properties properties = loadProperties();
        List<String> terms = loadTerms();

        StreamStage<Entry<String, Double>> tweetsWithSentiment = pipeline
                .drawFrom(StreamTwitterP.streamTwitter(properties, terms))
                .addTimestamps()
                .flatMap(JetCoinTrend::flatMapToRelevant)
                .customTransform("sentiment", SentimentProcessor::new);

        AggregateOperation1<Entry<String, Double>, ?, Tuple2<Double, Long>> aggrOp =
                allOf(averagingDouble(Entry::getValue), counting());

        tweetsWithSentiment.window(sliding(30_000, 10_000))
                           .groupingKey(entryKey())
                           .aggregate(aggrOp)
                           .drainTo(map(MAP_NAME_30_SECONDS));

        tweetsWithSentiment.window(sliding(60_000, 10_000))
                           .groupingKey(entryKey())
                           .aggregate(aggrOp)
                           .drainTo(map(MAP_NAME_1_MINUTE));

        tweetsWithSentiment.window(sliding(300_000, 10_000))
                           .groupingKey(entryKey())
                           .aggregate(aggrOp)
                           .drainTo(map(MAP_NAME_5_MINUTE));

        return pipeline;
    }


    // returns a traverser which flat maps each tweet to (coin, tweet) pairs by finding coins relevant to this tweet
    private static Traverser<? extends Entry<String, String>> flatMapToRelevant(String text) {
        AppendableTraverser<Entry<String, String>> traverser = new AppendableTraverser<>(4);
        for (String coin : CoinDefs.COIN_MAP.keySet()) {
            for (String keyword : CoinDefs.COIN_MAP.get(coin)) {
                if (text.contains(keyword)) {
                    traverser.append(entry(coin, text));
                }
            }
        }
        return traverser;
    }

    private static List<String> loadTerms() {
        List<String> terms = new ArrayList<>();
        CoinDefs.COIN_MAP.forEach((key, value) -> {
            terms.add(key);
            terms.addAll(value);
        });
        return terms;
    }

    private static Properties loadProperties() {
        Properties tokens = new Properties();
        try {
            tokens.load(JetCoinTrend.class.getResourceAsStream("twitter-security.properties"));
        } catch (IOException e) {
            throw rethrow(e);
        }
        return tokens;
    }


    private static void startConsolePrinterThread(JetInstance jet) {
        new Thread(() -> {
            Map<String, Tuple2<Double, Long>> map30secs = jet.getMap(MAP_NAME_30_SECONDS);
            Map<String, Tuple2<Double, Long>> map1min = jet.getMap(MAP_NAME_1_MINUTE);
            Map<String, Tuple2<Double, Long>> map5min = jet.getMap(MAP_NAME_5_MINUTE);

            while (running) {
                Set<String> coins = new HashSet<>();

                coins.addAll(map30secs.keySet());
                coins.addAll(map1min.keySet());
                coins.addAll(map5min.keySet());

                System.out.println("/------+---------------+---------------+----------------\\");
                System.out.println("|      |          Sentiment (tweet count)               |");
                System.out.println("| Coin | Last 30 sec   | Last minute   | Last 5 minutes |");
                System.out.println("|------+---------------+---------------+----------------|");
                coins.forEach((c) ->
                        System.out.format("| %s  | %s | %s | %s  |%n",
                                c, format(map30secs.get(c)), format(map1min.get(c)), format(map5min.get(c))));
                System.out.println("\\------+---------------+---------------+----------------/");

                LockSupport.parkNanos(MILLISECONDS.toNanos(PRINT_INTERNAL_MILLIS));
            }
        }).start();
    }

    private static String format(Tuple2<Double, Long> t) {
        if (t == null || t.f1() == 0) {
            return "             ";
        }
        String color = t.f0() > 0 ? ANSI_GREEN : t.f0() == 0 ? ANSI_YELLOW : ANSI_RED;
        return String.format("%s%7.4f (%3d)%s", color, t.f0(), t.f1(), ANSI_RESET);
    }
}
