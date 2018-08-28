package com.hazelcast.jet.demo;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.core.AppendableTraverser;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.demo.common.CoinDefs;
import com.hazelcast.jet.demo.common.SentimentAnalyzer;
import com.hazelcast.jet.demo.util.Util;
import com.hazelcast.jet.pipeline.ContextFactory;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.SourceBuilder.TimestampedSourceBuffer;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.StreamStageWithKey;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import edu.stanford.nlp.util.CoreMap;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.aggregate.AggregateOperations.allOf;
import static com.hazelcast.jet.aggregate.AggregateOperations.averagingDouble;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.demo.util.Util.*;
import static com.hazelcast.jet.function.DistributedFunctions.entryKey;
import static com.hazelcast.jet.pipeline.Sinks.map;
import static com.hazelcast.jet.pipeline.WindowDefinition.sliding;
import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;

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
 *  │    Calculate 5min      │   │    Calculate 30sec     │   │    Calculate 1min      │
 *  │Average with Event Count│   │Average with Event Count│   │Average with Event Count│
 *  └───────────┬────────────┘   └─────────────┬──────────┘   └───────────────┬────────┘
 *              │                              │                              │
 *              v                              v                              v
 *┌───────────────────────────┐ ┌─────────────────────────────┐ ┌───────────────────────────┐
 *│Write results to IMap(5Min)│ │Write results to IMap(30secs)│ │Write results to IMap(1Min)│
 *└───────────────────────────┘ └─────────────────────────────┘ └───────────────────────────┘
 */
public class CryptocurrencySentimentAnalysis {

    static {
        System.setProperty("hazelcast.logging.type", "log4j");
    }

    public static void main(String[] args) {
        System.out.println("DISCLAIMER: This is not investment advice");

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

    /**
     * Builds and returns the Pipeline which represents the actual computation.
     */
    private static Pipeline buildPipeline() {
        Pipeline pipeline = Pipeline.create();
        Properties properties = loadProperties();
        List<String> terms = loadTerms();

        StreamStageWithKey<Entry<String, Double>, String> tweetsWithSentiment = pipeline
                .drawFrom(twitterSource(properties, terms))
                .flatMap(CryptocurrencySentimentAnalysis::flatMapToRelevant)
                .mapUsingContext(ContextFactory
                                .withCreateFn(jet -> new SentimentAnalyzer())
                                .shareLocally(),
                        CryptocurrencySentimentAnalysis::calculateSentiment)
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

    /**
     * Calculates sentiment score for a coin and returns it as (coin,score) pair.
     *
     * @param analyzer NLP sentiment ingest
     * @param entry    (coin,tweet) pair
     */
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


    /**
     * Returns a traverser which flat maps each tweet to (coin, tweet) pairs
     * by finding coins relevant to this tweet
     *
     * @param text content of the tweet
     */
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


    private static StreamSource<String> twitterSource(Properties properties, List<String> terms) {
        return SourceBuilder.timestampedStream("twitter", ignored -> new TwitterSource(properties, terms))
                .fillBufferFn(TwitterSource::addToBuffer)
                .destroyFn(TwitterSource::destroy)
                .build();
    }

    private static class TwitterSource {

        private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(10000);
        private final ArrayList<String> buffer = new ArrayList<>();
        private final BasicClient client;

        TwitterSource(Properties properties, List<String> terms) {
            StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint().trackTerms(terms);

            String consumerKey = properties.getProperty("consumerKey");
            String consumerSecret = properties.getProperty("consumerSecret");
            String token = properties.getProperty("token");
            String tokenSecret = properties.getProperty("tokenSecret");

            if (isMissing(consumerKey) || isMissing(consumerSecret) || isMissing(token) || isMissing(tokenSecret)) {
                throw new IllegalArgumentException("Twitter credentials are missing!");
            }

            Authentication auth = new OAuth1(consumerKey, consumerSecret, token, tokenSecret);
            client = new ClientBuilder()
                    .hosts(Constants.STREAM_HOST)
                    .endpoint(endpoint)
                    .authentication(auth)
                    .processor(new StringDelimitedProcessor(queue))
                    .build();
            client.connect();
        }

        void addToBuffer(TimestampedSourceBuffer<String> sourceBuffer) {
            // drain everything at once for optimal performance and also naturally limit batch size
            queue.drainTo(buffer);
            for (String tweet : buffer) {
                JSONObject object = new JSONObject(tweet);
                if (object.has("text") && object.has("timestamp_ms")) {
                    String text = object.getString("text");
                    long timestamp = object.getLong("timestamp_ms");
                    sourceBuffer.add(text, timestamp);
                }
            }
            buffer.clear();
        }

        void destroy() {
            if (client != null) {
                client.stop();
            }
        }
    }
}
