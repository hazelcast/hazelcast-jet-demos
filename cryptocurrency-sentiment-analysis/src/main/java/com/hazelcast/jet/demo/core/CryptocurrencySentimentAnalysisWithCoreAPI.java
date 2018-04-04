package com.hazelcast.jet.demo.core;

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
import com.hazelcast.jet.core.processor.Processors;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.demo.common.CoinDefs;
import com.hazelcast.jet.demo.common.StreamTwitterP;
import com.hazelcast.jet.function.DistributedFunction;
import com.hazelcast.jet.function.DistributedToLongFunction;
import com.hazelcast.jet.pipeline.SlidingWindowDef;
import java.util.List;
import java.util.Properties;

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
import static com.hazelcast.jet.core.processor.Processors.mapP;
import static com.hazelcast.jet.core.processor.SinkProcessors.writeMapP;
import static com.hazelcast.jet.demo.util.Util.MAP_NAME_1_MINUTE;
import static com.hazelcast.jet.demo.util.Util.MAP_NAME_30_SECONDS;
import static com.hazelcast.jet.demo.util.Util.MAP_NAME_5_MINUTE;
import static com.hazelcast.jet.demo.util.Util.loadProperties;
import static com.hazelcast.jet.demo.util.Util.loadTerms;
import static com.hazelcast.jet.demo.util.Util.startConsolePrinterThread;
import static com.hazelcast.jet.demo.util.Util.stopConsolePrinterThread;
import static com.hazelcast.jet.function.DistributedFunctions.entryKey;
import static com.hazelcast.jet.pipeline.WindowDefinition.sliding;
import static java.util.Collections.singletonList;
/**
 * Twitter content is analyzed in real time to calculate cryptocurrency
 * trend list with its popularity index. The tweets are read from Twitter
 * and categorized by coin type (BTC, ETC, XRP, etc). In next step, NLP
 * sentimental analysis is applied to each tweet to calculate the sentiment
 * score of the respective tweet. This score says whether the Tweet has rather
 * positive or negative sentiment. Jet uses Stanford NLP lib to compute it.
 *
 * For each cryptocurrency, Jet aggregates scores from last 30 seconds,
 * last minute and last 5 minutes and prints the coin popularity table.
 *
 * The DAG used to model cryptocurrency calculations can be seen below :
 *
 *                                  ┌───────────────────┐
 *                                  │Twitter Data Source│
 *                                  └──────────┬────────┘
 *                                             │
 *                                             v
 *                                     ┌──────────────┐
 *                                     │Add Timestamps│
 *                                     └───────┬──────┘
 *                                             │
 *                                             v
 *                                 ┌──────────────────────┐
 *                                 │FlatMap Relevant Coins│
 *                                 └──────────┬───────────┘
 *                                            │
 *                                            v
 *                               ┌─────────────────────────┐
 *                               │Calculate Sentiment Score│
 *                               └─────────────┬───────────┘
 *                                             │
 *                                             v
 *                                   ┌──────────────────┐
 *                                   │Group by Coin Name│
 *                                   └────┬───┬─────┬───┘
 *                                        │   │     │
 *               ┌────────────────────────┘   │     └──────────────────────┐
 *               │                            │                            │
 *               v                            v                            v
 *  ┌────────────────────────┐   ┌────────────────────────┐   ┌────────────────────────┐
 *  │    Calcutate 5min      │   │    Calcutate 30sec     │   │    Calcutate 1min      │
 *  │Average with Event Count│   │Average with Event Count│   │Average with Event Count│
 *  └───────────┬────────────┘   └─────────────┬──────────┘   └───────────────┬────────┘
 *              │                              │                              │
 *              v                              v                              v
 *┌───────────────────────────┐ ┌─────────────────────────────┐ ┌───────────────────────────┐
 *│Write results to IMap(5Min)│ │Write results to IMap(30secs)│ │Write results to IMap(1Min)│
 *└───────────────────────────┘ └─────────────────────────────┘ └───────────────────────────┘
 */
public class CryptocurrencySentimentAnalysisWithCoreAPI {

    static {
        System.setProperty("hazelcast.logging.type", "log4j");
    }

    public static void main(String[] args) {
        System.out.println("DISCLAIMER: This is not an investment advice");

        DAG dag = buildDag();
        // Start Jet
        JetInstance jet = Jet.newJetInstance();
        startConsolePrinterThread(jet);
        try {
            // Perform the computation
            jet.newJob(dag).join();
        } finally {
            stopConsolePrinterThread();
            Jet.shutdownAll();
        }
    }

    /**
     * Builds and returns the DAG which represents the actual computation.
     */
    private static DAG buildDag() {
        DAG dag = new DAG();

        Properties properties = loadProperties();
        List<String> terms = loadTerms();
        Vertex twitterSource = dag.newVertex("twitter", StreamTwitterP.streamTwitterP(properties, terms));
        Vertex addTimestamps = dag.newVertex("addTimestamps", mapP(string -> new TimestampedEntry<>(System.currentTimeMillis(), null, string)));
        Vertex relevance = dag.newVertex("relevance", Processors.<TimestampedEntry<Object, String>,
                TimestampedEntry<String, String>>flatMapP(CryptocurrencySentimentAnalysisWithCoreAPI::flatMapToRelevant));
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

        return dag.edge(between(twitterSource, addTimestamps))
                  .edge(between(addTimestamps, insertWm))
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

}
