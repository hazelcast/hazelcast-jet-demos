/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
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

import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.ContextFactory;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;
import support.WordIndex;

import java.util.Map;

import static com.hazelcast.jet.datamodel.Tuple2.tuple2;

/**
 * Shows how to enrich a stream of movie reviews with classification using
 * a pre-trained TensorFlow model. Executes the TensorFlow model using the
 * in-process method.
 * TensorFlow Model Server execution.
 */
public class InProcessClassification {

    private static Pipeline buildPipeline(String dataPath, IMap<Long, String> reviewsMap) {
        WordIndex wordIndex = new WordIndex(dataPath);
        // Set up the mapping context that loads the model on each member, shared
        // by all parallel processors on that member. The path must be available on
        // all members.
        ContextFactory<SavedModelBundle> modelContext = ContextFactory
                .withCreateFn(jet -> SavedModelBundle.load(dataPath + "/model/1", "serve"))
                .withLocalSharing()
                .withDestroyFn(SavedModelBundle::close);
        Pipeline p = Pipeline.create();
        p.drawFrom(Sources.map(reviewsMap))
         .map(Map.Entry::getValue)
         .mapUsingContext(modelContext, (model, review) -> classify(review, model, wordIndex))
         // TensorFlow executes models in parallel, we'll use 2 local threads to maximize throughput.
         .setLocalParallelism(2)
         .drainTo(Sinks.logger(t -> String.format("Sentiment rating for review \"%s\" is %.2f", t.f0(), t.f1())));
        return p;
    }

    public static void main(String[] args) {
        System.setProperty("hazelcast.logging.type", "log4j");

        if (args.length != 1) {
            System.out.println("Usage: InProcessClassification <data path>");
            System.exit(1);
        }

        JetInstance instance = Jet.newJetInstance();
        try {
            IMap<Long, String> reviewsMap = instance.getMap("reviewsMap");
            SampleReviews.populateReviewsMap(reviewsMap);
            instance.newJob(buildPipeline(args[0], reviewsMap)).join();
        } finally {
            instance.shutdown();
        }
    }

    private static Tuple2<String, Float> classify(
            String review, SavedModelBundle model, WordIndex wordIndex
    ) {
        try (Tensor<Float> input = Tensors.create(wordIndex.createTensorInput(review));
             Tensor<?> output = model.session().runner()
                                     .feed("embedding_input:0", input)
                                     .fetch("dense_1/Sigmoid:0").run().get(0)
        ) {
            float[][] result = new float[1][1];
            output.copyTo(result);
            return tuple2(review, result[0][0]);
        }
    }
}
