import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Processor;
import com.hazelcast.jet.core.TimestampKind;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.WatermarkGenerationParams;
import com.hazelcast.jet.core.WindowDefinition;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.function.DistributedBiFunction;
import com.hazelcast.jet.function.DistributedFunction;
import com.hazelcast.jet.function.DistributedSupplier;
import com.hazelcast.jet.function.DistributedToDoubleFunction;
import com.hazelcast.jet.function.DistributedToLongFunction;
import java.util.concurrent.locks.LockSupport;
import org.jetbrains.annotations.NotNull;

import static com.hazelcast.jet.aggregate.AggregateOperations.averagingDouble;
import static com.hazelcast.jet.core.Edge.between;
import static com.hazelcast.jet.core.Edge.from;
import static com.hazelcast.jet.core.ProcessorMetaSupplier.dontParallelize;
import static com.hazelcast.jet.core.ProcessorSupplier.of;
import static com.hazelcast.jet.core.WatermarkEmissionPolicy.emitByFrame;
import static com.hazelcast.jet.core.WatermarkPolicies.withFixedLag;
import static com.hazelcast.jet.core.WindowDefinition.slidingWindowDef;
import static com.hazelcast.jet.core.processor.Processors.aggregateToSlidingWindowP;
import static com.hazelcast.jet.core.processor.Processors.insertWatermarksP;
import static com.hazelcast.jet.core.processor.SinkProcessors.updateMapP;

public class JetCoinTrend {

    private static final String MAP_NAME_30_SECONDS = "map30Seconds";
    private static final String MAP_NAME_1_MINUTE = "map1Min";
    private static final String MAP_NAME_5_MINUTE = "map5Min";
    private static final int PRINT_INTERNAL_MILLIS = 30000;

    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "log4j");
        System.out.println("DISCLAIMER: This is not an investment advice");

        DAG dag = buildDag();
        // Start Jet
        JetInstance jet = Jet.newJetInstance();
        startConsolePrinterThread(jet);
        try {
            // Perform the computation
            jet.newJob(dag).join();
        } finally {
            Jet.shutdownAll();
        }
    }

    private static void startConsolePrinterThread(JetInstance jet) {
        new Thread(() -> {
            long lastPrint = 0;
            while (true) {
                long now = System.currentTimeMillis();
                if (now > lastPrint + PRINT_INTERNAL_MILLIS) {
                    lastPrint = now;
                    System.out.println("----------------");
                    System.out.println("30 Second Aggregations");
                    System.out.println("----------------");
                    jet.getMap(MAP_NAME_30_SECONDS).forEach((k, v) -> System.out.println(k + " -> " + v));
                    System.out.println("----------------");
                    System.out.println("1 Minute Aggregations");
                    System.out.println("----------------");
                    jet.getMap(MAP_NAME_1_MINUTE).forEach((k, v) -> System.out.println(k + " -> " + v));
                    System.out.println("----------------");
                    System.out.println("5 Minute Aggregations");
                    System.out.println("----------------");
                    jet.getMap(MAP_NAME_5_MINUTE).forEach((k, v) -> System.out.println(k + " -> " + v));
                }
                LockSupport.parkNanos(1000);
            }
        }).start();
    }

    @NotNull
    private static DAG buildDag() {
        DAG dag = new DAG();
        Vertex twitterSource = dag.newVertex("twitter", dontParallelize(of(TwitterSource::new)));
        Vertex relevance = dag.newVertex("relevance", of(RelevanceProcessor::new));
        Vertex sentiment = dag.newVertex("sentiment", of(SentimentProcessor::new));

        WindowDefinition slidingWindowDef30Secs = slidingWindowDef(30_000, 10_000);
        WindowDefinition slidingWindowDef1Min = slidingWindowDef(60_000, 30_000);
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


        Vertex map30Seconds = dag.newVertex(MAP_NAME_30_SECONDS, updateMapP(MAP_NAME_30_SECONDS,
                TimestampedEntry::getKey,
                (DistributedBiFunction<Double, TimestampedEntry<String, Double>, Double>) (oldValue, item) -> item.getValue()
                )
        );
        Vertex map1Min = dag.newVertex(MAP_NAME_1_MINUTE, updateMapP(MAP_NAME_1_MINUTE,
                TimestampedEntry::getKey,
                (DistributedBiFunction<Double, TimestampedEntry<String, Double>, Double>) (oldValue, item) -> item.getValue()
                )

        );
        Vertex map5Min = dag.newVertex(MAP_NAME_5_MINUTE, updateMapP(MAP_NAME_5_MINUTE,
                TimestampedEntry::getKey,
                (DistributedBiFunction<Double, TimestampedEntry<String, Double>, Double>) (oldValue, item) -> item.getValue()
                )
        );

        dag.edge(between(twitterSource, insertWm));
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
        dag.edge(from(aggregateTrend30Secs, 1).to(map30Seconds));
        dag.edge(from(aggregateTrend1Min, 1).to(map1Min));
        dag.edge(from(aggregateTrend5Min).to(map5Min));
        return dag;
    }


}
