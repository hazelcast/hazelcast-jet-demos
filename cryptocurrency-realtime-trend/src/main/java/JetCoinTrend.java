import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Processor;
import com.hazelcast.jet.core.TimestampKind;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.WatermarkGenerationParams;
import com.hazelcast.jet.core.WindowDefinition;
import com.hazelcast.jet.core.processor.DiagnosticProcessors;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.function.DistributedFunction;
import com.hazelcast.jet.function.DistributedSupplier;
import com.hazelcast.jet.function.DistributedToDoubleFunction;
import com.hazelcast.jet.function.DistributedToLongFunction;

import static com.hazelcast.jet.aggregate.AggregateOperations.averagingDouble;
import static com.hazelcast.jet.core.Edge.between;
import static com.hazelcast.jet.core.Edge.from;
import static com.hazelcast.jet.core.ProcessorMetaSupplier.dontParallelize;
import static com.hazelcast.jet.core.ProcessorSupplier.of;
import static com.hazelcast.jet.core.WatermarkEmissionPolicy.emitByFrame;
import static com.hazelcast.jet.core.WatermarkPolicies.withFixedLag;
import static com.hazelcast.jet.core.WindowDefinition.*;
import static com.hazelcast.jet.core.processor.Processors.aggregateToSlidingWindowP;
import static com.hazelcast.jet.core.processor.Processors.insertWatermarksP;

public class JetCoinTrend {
    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type","log4j");
        System.out.println("DISCLAIMER: This is not an investment advice");

        DAG dag = new DAG();
        Vertex twitterSource = dag.newVertex("twitter", dontParallelize(of(TwitterSource::new)));
//        Vertex redditSource = dag.newVertex("reddit", dontParallelize(new RedditSource()));
        Vertex relevance = dag.newVertex("relevance", of(RelevanceProcessor::new));
        Vertex sentiment = dag.newVertex("sentiment", of(SentimentProcessor::new));

        WindowDefinition slidingWindowDef30Secs = slidingWindowDef(10_000, 5000);
        WindowDefinition slidingWindowDef1Min = slidingWindowDef(60_000, 10_000);
        WindowDefinition slidingWindowDef5Min = slidingWindowDef(300_000, 60_000);

        WatermarkGenerationParams<TimestampedEntry<String, Double>> params = WatermarkGenerationParams
                .wmGenParams(
                        (DistributedToLongFunction<TimestampedEntry<String, Double>>) TimestampedEntry::getTimestamp,
                        withFixedLag(5000),
                        emitByFrame(slidingWindowDef30Secs), 60000
                );
        DistributedSupplier<Processor> insertWMP = insertWatermarksP(params);

        Vertex insertWm = dag.newVertex("insertWm", insertWMP).localParallelism(1);

        Vertex aggregateTrend30Secs = dag.newVertex("aggregateTrend30Secs", aggregateToSlidingWindowP(
                TimestampedEntry::getKey,
                TimestampedEntry::getTimestamp,
                TimestampKind.EVENT,
                slidingWindowDef30Secs,
                averagingDouble(
                        (DistributedToDoubleFunction<TimestampedEntry<String, Double>>) TimestampedEntry::getValue)
                )
        );

        Vertex aggregateTrend1Min = dag.newVertex("aggregateTrend1Min", aggregateToSlidingWindowP(
                TimestampedEntry::getKey,
                TimestampedEntry::getTimestamp,
                TimestampKind.EVENT,
                slidingWindowDef1Min,
                averagingDouble(
                        (DistributedToDoubleFunction<TimestampedEntry<String, Double>>) TimestampedEntry::getValue)
                )
        );

        Vertex aggregateTrend5Min = dag.newVertex("aggregateTrend5Min", aggregateToSlidingWindowP(
                TimestampedEntry::getKey,
                TimestampedEntry::getTimestamp,
                TimestampKind.EVENT,
                slidingWindowDef5Min,
                averagingDouble(
                        (DistributedToDoubleFunction<TimestampedEntry<String, Double>>) TimestampedEntry::getValue)
                )
        );


        Vertex sink = dag.newVertex("sink", DiagnosticProcessors.writeLoggerP());

        dag.edge(between(twitterSource, insertWm));
//        dag.edge(Edge.between(redditSource, insertWm));
        dag.edge(between(insertWm, relevance));
        dag.edge(between(relevance, sentiment));
        dag.edge(from(sentiment).to(aggregateTrend30Secs).
                partitioned(
                        (DistributedFunction<TimestampedEntry<String, Double>, Object>) TimestampedEntry::getKey
                ).distributed()
        );
        dag.edge(from(aggregateTrend30Secs).to(aggregateTrend1Min).
                partitioned(
                        (DistributedFunction<TimestampedEntry<String, Double>, Object>) TimestampedEntry::getKey
                ).distributed()
        );
        dag.edge(from(aggregateTrend1Min).to(aggregateTrend5Min).
                partitioned(
                        (DistributedFunction<TimestampedEntry<String, Double>, Object>) TimestampedEntry::getKey
                ).distributed()
        );
        dag.edge(from(aggregateTrend30Secs, 1).to(sink, 0));
        dag.edge(from(aggregateTrend1Min, 1).to(sink, 1));
        dag.edge(from(aggregateTrend5Min).to(sink, 2));

        // Start Jet, populate the input list
        JetInstance jet = Jet.newJetInstance();
        try {

            // Perform the computation
            jet.newJob(dag).join();

        } finally {
            Jet.shutdownAll();
        }
    }


}
