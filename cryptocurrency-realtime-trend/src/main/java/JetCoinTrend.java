import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.core.AppendableTraverser;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.TimestampKind;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.WatermarkGenerationParams;
import com.hazelcast.jet.core.processor.DiagnosticProcessors;
import com.hazelcast.jet.core.processor.Processors;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.function.DistributedFunction;
import com.hazelcast.jet.function.DistributedToLongFunction;
import com.hazelcast.jet.pipeline.SlidingWindowDef;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

import static com.hazelcast.jet.aggregate.AggregateOperations.averagingDouble;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.core.Edge.between;
import static com.hazelcast.jet.core.Edge.from;
import static com.hazelcast.jet.core.ProcessorSupplier.of;
import static com.hazelcast.jet.core.WatermarkEmissionPolicy.emitByFrame;
import static com.hazelcast.jet.core.WatermarkGenerationParams.wmGenParams;
import static com.hazelcast.jet.core.WatermarkPolicies.limitingLag;
import static com.hazelcast.jet.core.processor.Processors.aggregateToSlidingWindowP;
import static com.hazelcast.jet.core.processor.Processors.insertWatermarksP;
import static com.hazelcast.jet.core.processor.SinkProcessors.writeMapP;
import static com.hazelcast.jet.function.DistributedFunctions.entryKey;
import static com.hazelcast.jet.pipeline.WindowDefinition.sliding;
import static com.hazelcast.util.ExceptionUtil.rethrow;
import static java.util.Collections.singletonList;
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

    public static void main(String[] args) throws Exception {
        System.out.println("DISCLAIMER: This is not an investment advice");

        DAG dag = buildDag();
        // Start Jet
        JetInstance jet = Jet.newJetInstance();
        startConsolePrinterThread(jet);
        try {
            // Perform the computation
            jet.newJob(dag).join();
        } finally {
            running = false;
            Jet.shutdownAll();
        }
    }

    private static DAG buildDag() {
        DAG dag = new DAG();

        Properties properties = loadProperties();
        List<String> terms = loadTerms();
        Vertex twitterSource = dag.newVertex("twitter", StreamTwitterP.streamTwitterP(properties, terms));
        Vertex relevance = dag.newVertex("relevance", Processors.<TimestampedEntry<Object, String>,
                TimestampedEntry<String, String>>flatMapP(JetCoinTrend::flatMapToRelevant));
        Vertex sentiment = dag.newVertex("sentiment", of(SentimentProcessor::new));

        SlidingWindowDef slidingWindowOf30Sec = sliding(30_000, 10_000);
        SlidingWindowDef slidingWindowOf1Min = sliding(60_000, 10_000);
        SlidingWindowDef slidingWindowOf5Min = sliding(300_000, 10_000);

        WatermarkGenerationParams<TimestampedEntry<String, Double>> params = wmGenParams(
                TimestampedEntry::getTimestamp,
                limitingLag(5000),
                emitByFrame(slidingWindowOf30Sec.toSlidingWindowPolicy()), 60000
        );
        Vertex insertWm = dag.newVertex("insertWm", insertWatermarksP(params)).localParallelism(1);

        AggregateOperation1<TimestampedEntry<String, Double>, ?, Tuple2<Double, Long>> aggrOp =
                AggregateOperations.allOf(averagingDouble(TimestampedEntry::getValue), counting());


        DistributedFunction<TimestampedEntry, Object> getKeyFn = TimestampedEntry::getKey;
        DistributedToLongFunction<TimestampedEntry> getTimeStampFn = TimestampedEntry::getTimestamp;
        Vertex slidingWin30sec = dag.newVertex("slidingWin30Sec", aggregateToSlidingWindowP(
                singletonList(getKeyFn),
                singletonList(getTimeStampFn),
                TimestampKind.EVENT,
                slidingWindowOf30Sec.toSlidingWindowPolicy(),
                aggrOp,
                (ignored, timestamp, key, value) -> new TimestampedEntry<>(ignored, timestamp, key, value)
        ));

        Vertex slidingWin1min = dag.newVertex("slidingWin1Min", aggregateToSlidingWindowP(
                singletonList(getKeyFn),
                singletonList(getTimeStampFn),
                TimestampKind.EVENT,
                slidingWindowOf1Min.toSlidingWindowPolicy(),
                aggrOp,
                (ignored, timestamp, key, value) -> new TimestampedEntry<>(ignored, timestamp, key, value)
        ));

        Vertex slidingWin5min = dag.newVertex("slidingWin5Min", aggregateToSlidingWindowP(
                singletonList(getKeyFn),
                singletonList(getTimeStampFn),
                TimestampKind.EVENT,
                slidingWindowOf5Min.toSlidingWindowPolicy(),
                aggrOp,
                (ignored, timestamp, key, value) -> new TimestampedEntry<>(ignored, timestamp, key, value)
        ));

        Vertex map30Seconds = dag.newVertex(MAP_NAME_30_SECONDS, writeMapP(MAP_NAME_30_SECONDS));
        Vertex map1Min = dag.newVertex(MAP_NAME_1_MINUTE, writeMapP(MAP_NAME_1_MINUTE));
        Vertex map5Min = dag.newVertex(MAP_NAME_5_MINUTE, writeMapP(MAP_NAME_5_MINUTE));

        Vertex loggerSink = dag.newVertex("logger", DiagnosticProcessors.writeLoggerP());

        return dag.edge(between(twitterSource, insertWm))
                  .edge(between(insertWm, relevance))
                  .edge(between(relevance, sentiment))
                  .edge(from(sentiment, 0).to(slidingWin30sec).partitioned(entryKey()).distributed())
                  .edge(from(sentiment, 1).to(slidingWin1min).partitioned(entryKey()).distributed())
                  .edge(from(sentiment, 2).to(slidingWin5min).partitioned(entryKey()).distributed())
                  .edge(between(slidingWin30sec, map30Seconds))
                  .edge(between(slidingWin1min, map1Min))
                  .edge(between(slidingWin5min, map5Min));
    }

    // returns a traverser which flat maps each tweet to (coin, tweet) pairs by finding coins relevant to this tweet
    private static Traverser<? extends TimestampedEntry<String, String>> flatMapToRelevant(TimestampedEntry<Object, String> e) {
        AppendableTraverser<TimestampedEntry<String, String>> traverser = new AppendableTraverser<>(4);
        String text = e.getValue();
        for (String coin : CoinDefs.COIN_MAP.keySet()) {
            for (String keyword : CoinDefs.COIN_MAP.get(coin)) {
                if (text.contains(keyword)) {
                    traverser.append(new TimestampedEntry<>(e.getTimestamp(), coin, e.getValue()));
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
