package com.hazelcast.jet.demo;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.core.AppendableTraverser;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.demo.common.CoinDefs;
import com.hazelcast.jet.demo.common.SentimentAnalyzer;
import com.hazelcast.jet.demo.common.StreamTwitterP;
import com.hazelcast.jet.demo.util.Util;
import com.hazelcast.jet.pipeline.ContextFactory;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.StreamStageWithGrouping;
import edu.stanford.nlp.util.CoreMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import javax.annotation.Nullable;

import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.aggregate.AggregateOperations.allOf;
import static com.hazelcast.jet.aggregate.AggregateOperations.averagingDouble;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.demo.util.Util.MAP_NAME_1_MINUTE;
import static com.hazelcast.jet.demo.util.Util.MAP_NAME_30_SECONDS;
import static com.hazelcast.jet.demo.util.Util.MAP_NAME_5_MINUTE;
import static com.hazelcast.jet.demo.util.Util.loadProperties;
import static com.hazelcast.jet.demo.util.Util.loadTerms;
import static com.hazelcast.jet.demo.util.Util.startConsolePrinterThread;
import static com.hazelcast.jet.function.DistributedFunctions.entryKey;
import static com.hazelcast.jet.pipeline.Sinks.map;
import static com.hazelcast.jet.pipeline.WindowDefinition.sliding;
import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;

public class JetCoinTrend {

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
            Util.stopConsolePrinterThread();
            Jet.shutdownAll();
        }
    }

    private static Pipeline buildPipeline() {
        Pipeline pipeline = Pipeline.create();
        Properties properties = loadProperties();
        List<String> terms = loadTerms();

        StreamStageWithGrouping<Entry<String, Double>, String> tweetsWithSentiment = pipeline
                .drawFrom(StreamTwitterP.streamTwitter(properties, terms))
                .addTimestamps()
                .flatMap(JetCoinTrend::flatMapToRelevant)
                .mapUsingContext(ContextFactory.withCreateFn(jet -> new SentimentAnalyzer()),
                        JetCoinTrend::calculateSentiment)
                .groupingKey(entryKey());

        AggregateOperation1<Entry<String, Double>, ?, Tuple2<Double, Long>> aggrOp =
                allOf(averagingDouble(Entry::getValue), counting());

        tweetsWithSentiment.window(sliding(30_000, 10_000))
                           .aggregate(aggrOp)
                           .drainTo(map(MAP_NAME_30_SECONDS));

        tweetsWithSentiment.window(sliding(60_000, 10_000))
                           .aggregate(aggrOp)
                           .drainTo(map(MAP_NAME_1_MINUTE));

        tweetsWithSentiment.window(sliding(300_000, 10_000))
                           .aggregate(aggrOp)
                           .drainTo(map(MAP_NAME_5_MINUTE));

        return pipeline;
    }

    @Nullable
    private static Entry<String, Double> calculateSentiment(SentimentAnalyzer analyzer, Entry<String, String> entry) {
        List<CoreMap> annotations = analyzer.getAnnotations(entry.getValue());
        double sentimentType = analyzer.getSentimentClass(annotations);
        double sentimentScore = analyzer.getScore(annotations, sentimentType);

        double score = sentimentType * sentimentScore;
        if (isNaN(score) || isInfinite(score)) {
            return null;
        }
        return entry(entry.getKey(), score);
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


}
