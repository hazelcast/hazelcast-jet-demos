/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.pipeline.Pipeline;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;

import static com.hazelcast.jet.aggregate.AggregateOperations.maxBy;
import static com.hazelcast.jet.function.DistributedComparator.comparingDouble;
import static com.hazelcast.jet.pipeline.WindowDefinition.tumbling;

/**
 * An application which uses webcam frame stream as input and classifies those frames
 * with a model pre-trained with CIFAR-10 dataset.
 * Frames constituting a second of stream will be aggregated together to find
 * maximum scored classification and that will be sent a GUI sink to be shown on the screen.
 */
public class RealtimeImageRecognition {

    static {
        System.setProperty("hazelcast.logging.type", "slf4j");
    }

    public static void main(String[] args) {
        Path modelPath = Paths.get(args[0]).toAbsolutePath();

        Pipeline pipeline = buildPipeline(modelPath.toString());

        JetInstance jet = Jet.newJetInstance();
        try {
            jet.newJob(pipeline).join();
        } finally {
            Jet.shutdownAll();
        }
    }

    private static Pipeline buildPipeline(String modelPath) {
        Pipeline pipeline = Pipeline.create();
        pipeline.drawFrom(WebcamSource.webcam())
                .addTimestamps()
                .<Entry<SerializableBufferedImage, Entry<String, Double>>>
                        customTransform("classifier", () -> new ClassifierProcessor(modelPath))
                .window(tumbling(1000))
                .aggregate(maxBy(comparingDouble(e -> e.getValue().getValue())))
                .drainTo(GUISink.sink());
        return pipeline;
    }

}
