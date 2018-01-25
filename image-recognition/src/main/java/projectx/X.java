package projectx;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Partitioner;
import com.hazelcast.jet.core.Processor;
import com.hazelcast.jet.core.TimestampKind;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.WindowDefinition;
import com.hazelcast.jet.function.DistributedSupplier;
import com.hazelcast.jet.function.DistributedToLongFunction;
import com.hazelcast.jet.server.JetBootstrap;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.Map.Entry;

import static com.hazelcast.jet.aggregate.AggregateOperations.maxBy;
import static com.hazelcast.jet.core.Edge.between;
import static com.hazelcast.jet.core.ProcessorSupplier.of;
import static com.hazelcast.jet.core.WatermarkEmissionPolicy.emitByFrame;
import static com.hazelcast.jet.core.WatermarkGenerationParams.wmGenParams;
import static com.hazelcast.jet.core.WatermarkPolicies.withFixedLag;
import static com.hazelcast.jet.core.processor.Processors.aggregateToSlidingWindowP;
import static com.hazelcast.jet.core.processor.Processors.insertWatermarksP;
import static com.hazelcast.jet.function.DistributedComparator.comparingDouble;

public class X {

    public static void main(String[] args) throws Exception {
        JobConfig config = new JobConfig();
        config.addResource(Paths.get("download_data.zip").toFile());

        DAG dag = buildDAG();

        JetInstance jet = JetBootstrap.getInstance();
        jet.newJob(dag, config).join();
    }


    private static DAG buildDAG() {
        DAG dag = new DAG();

        Vertex webcamSource = dag.newVertex("webcam source", WebcamSource.webcam());
        Vertex classifierVertex = dag.newVertex("classifier", of(ClassifierProcessor::new));

        WindowDefinition tumbling = WindowDefinition.tumblingWindowDef(1000);
        DistributedSupplier<Processor> insertWMP = insertWatermarksP(wmGenParams(
                (DistributedToLongFunction<Object>) value -> System.currentTimeMillis(),
                withFixedLag(100),
                emitByFrame(tumbling),
                60000L
        ));
        Vertex insertWm = dag.newVertex("insertWm", insertWMP).localParallelism(1);

        Vertex maxScore = dag.newVertex("aggregateTrend", aggregateToSlidingWindowP(
                (Entry<BufferedImage, Entry<String, Double>> input) -> "MAX_SCORE",
                (Entry<BufferedImage, Entry<String, Double>> input) -> System.currentTimeMillis(),
                TimestampKind.EVENT,
                tumbling,
                maxBy(comparingDouble((Entry<BufferedImage, Entry<String, Double>> input) -> {
                            Entry<String, Double> maxScoredCategory = input.getValue();
                            System.out.println("maxScoredCategory = " + maxScoredCategory);
                            return maxScoredCategory.getValue();
                        })
                )));

        Vertex guiSink = dag.newVertex("gui", GUISink.sink());

        dag.edge(between(webcamSource, classifierVertex))
           .edge(between(classifierVertex, insertWm))
           .edge(between(insertWm, maxScore).partitioned(t -> "gui", Partitioner.HASH_CODE).distributed())
           .edge(between(maxScore, guiSink));
        return dag;
    }

}
