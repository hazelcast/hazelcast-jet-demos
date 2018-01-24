import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Edge;
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
import static com.hazelcast.jet.core.ProcessorMetaSupplier.dontParallelize;
import static com.hazelcast.jet.core.ProcessorSupplier.of;
import static com.hazelcast.jet.core.WatermarkEmissionPolicy.emitByFrame;
import static com.hazelcast.jet.core.WatermarkPolicies.withFixedLag;
import static com.hazelcast.jet.core.processor.Processors.aggregateToSlidingWindowP;
import static com.hazelcast.jet.core.processor.Processors.insertWatermarksP;

public class JetCoinTrend {
    public static void main(String[] args) throws Exception {
        System.out.println("NOT AN INVESTMENT SUGGESTION");

        DAG dag = new DAG();
        Vertex twitterSource = dag.newVertex("twitter", dontParallelize(new TwitterSource()));
//        Vertex redditSource = dag.newVertex("reddit", dontParallelize(new RedditSource()));
        Vertex relevance = dag.newVertex("relevance", of(RelevanceProcessor::new));
        Vertex sentiment = dag.newVertex("sentiment", of(SentimentProcessor::new));

        WindowDefinition slidingWindowDef1Min = WindowDefinition.slidingWindowDef(10000, 5000);
        WindowDefinition slidingWindowDef5Min = WindowDefinition.slidingWindowDef(300000, 60000);
        WindowDefinition slidingWindowDef15Min = WindowDefinition.slidingWindowDef(900000, 300000);

        WatermarkGenerationParams<TimestampedEntry<String, Double>> params = WatermarkGenerationParams.wmGenParams((DistributedToLongFunction<TimestampedEntry<String, Double>>) TimestampedEntry::getTimestamp, withFixedLag(5000), emitByFrame(slidingWindowDef1Min), 60000);
        DistributedSupplier<Processor> insertWMP = insertWatermarksP(params);

        Vertex insertWm = dag.newVertex("insertWm", insertWMP).localParallelism(1);

        Vertex aggregateTrend1Min = dag.newVertex("aggregateTrend1Min", aggregateToSlidingWindowP(TimestampedEntry::getKey,
                TimestampedEntry::getTimestamp,
                TimestampKind.EVENT,
                slidingWindowDef1Min, averagingDouble((DistributedToDoubleFunction<TimestampedEntry<String, Double>>) TimestampedEntry::getValue)
        ));

        Vertex aggregateTrend5Min = dag.newVertex("aggregateTrend5Min", aggregateToSlidingWindowP(TimestampedEntry::getKey,
                TimestampedEntry::getTimestamp,
                TimestampKind.EVENT,
                slidingWindowDef5Min, averagingDouble(
                        (DistributedToDoubleFunction<TimestampedEntry<String, Double>>) TimestampedEntry::getValue)
        ));

        Vertex aggregateTrend15Min = dag.newVertex("aggregateTrend15Min", aggregateToSlidingWindowP(TimestampedEntry::getKey,
                TimestampedEntry::getTimestamp,
                TimestampKind.EVENT,
                slidingWindowDef15Min, averagingDouble(
                        (DistributedToDoubleFunction<TimestampedEntry<String, Double>>) TimestampedEntry::getValue)
        ));


        Vertex sink = dag.newVertex("sink", DiagnosticProcessors.writeLoggerP());

        dag.edge(Edge.between(twitterSource, insertWm));
//        dag.edge(Edge.between(redditSource, insertWm));
        dag.edge(Edge.between(insertWm, relevance));
        dag.edge(Edge.between(relevance, sentiment));
        dag.edge(Edge.from(sentiment, 0).to(aggregateTrend1Min).partitioned(
                (DistributedFunction<TimestampedEntry<String, Double>, Object>) TimestampedEntry::getKey));
        dag.edge(Edge.from(sentiment, 1).to(aggregateTrend5Min).partitioned(
                (DistributedFunction<TimestampedEntry<String, Double>, Object>) TimestampedEntry::getKey));
        dag.edge(Edge.from(sentiment, 2).to(aggregateTrend15Min).partitioned(
                (DistributedFunction<TimestampedEntry<String, Double>, Object>) TimestampedEntry::getKey));
        dag.edge(Edge.from(aggregateTrend1Min).to(sink, 0));
        dag.edge(Edge.from(aggregateTrend5Min).to(sink, 1));
        dag.edge(Edge.from(aggregateTrend15Min).to(sink, 2));


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