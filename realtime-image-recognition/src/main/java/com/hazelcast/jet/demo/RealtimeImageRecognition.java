package com.hazelcast.jet.demo;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Processor;
import com.hazelcast.jet.core.TimestampKind;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.WindowDefinition;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.function.DistributedSupplier;
import com.hazelcast.jet.function.DistributedToLongFunction;
import com.hazelcast.jet.server.JetBootstrap;
import java.net.URL;
import java.util.Map.Entry;

import static com.hazelcast.jet.aggregate.AggregateOperations.maxBy;
import static com.hazelcast.jet.core.Edge.between;
import static com.hazelcast.jet.core.ProcessorSupplier.of;
import static com.hazelcast.jet.core.WatermarkEmissionPolicy.emitByFrame;
import static com.hazelcast.jet.core.WatermarkGenerationParams.wmGenParams;
import static com.hazelcast.jet.core.WatermarkPolicies.withFixedLag;
import static com.hazelcast.jet.core.processor.DiagnosticProcessors.peekInputP;
import static com.hazelcast.jet.core.processor.Processors.accumulateByFrameP;
import static com.hazelcast.jet.core.processor.Processors.combineToSlidingWindowP;
import static com.hazelcast.jet.core.processor.Processors.insertWatermarksP;
import static com.hazelcast.jet.function.DistributedComparator.comparingDouble;

/**
 * An application which uses webcam frame stream as input and classifies those frames
 * with a model pre-trained with CIFAR-10 dataset.
 * Frames constituting a second of stream will be aggregated together to find
 * maximum scored classification and that will be sent a GUI sink to be shown on the screen.
 */
public class RealtimeImageRecognition {

    public static void main(String[] args) throws Exception {
        JobConfig config = new JobConfig();

        config.addResource(new URL("http://boofcv.org/notwiki/largefiles/likevgg_cifar10.zip"));

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
                (DistributedToLongFunction<TimestampedEntry>) TimestampedEntry::getTimestamp,
                withFixedLag(500),
                emitByFrame(tumbling),
                60000L
        ));
        Vertex insertWm = dag.newVertex("insertWm", insertWMP);

        Vertex localMaxScore = dag.newVertex("localMaxScore", accumulateByFrameP(
                (TimestampedEntry<SerializableBufferedImage, Entry<String, Double>> input) -> "MAX_SCORE",
                TimestampedEntry::getTimestamp,
                TimestampKind.EVENT,
                tumbling,
                maxBy(comparingDouble((Entry<SerializableBufferedImage, Entry<String, Double>> input) -> {
                            Entry<String, Double> maxScoredCategory = input.getValue();
                            return maxScoredCategory.getValue();
                        })
                ))).localParallelism(1);
        Vertex globalMaxScore = dag.newVertex("globalMaxScore", peekInputP(combineToSlidingWindowP(
                tumbling,
                maxBy(comparingDouble((Entry<SerializableBufferedImage, Entry<String, Double>> input)-> {
                            Entry<String, Double> maxScoredCategory = input.getValue();
                            return maxScoredCategory.getValue();
                        })
                )))).localParallelism(1);

        Vertex guiSink = dag.newVertex("gui", peekInputP(GUISink.sink()));

        dag.edge(between(webcamSource, classifierVertex))
           .edge(between(classifierVertex, insertWm))
           .edge(between(insertWm, localMaxScore).partitioned(o -> "MAX_SCORE"))
           .edge(between(localMaxScore, globalMaxScore).partitioned(input -> "gui").distributed())
           .edge(between(globalMaxScore, guiSink).partitioned(input -> "gui"));
        return dag;
    }

}
