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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Int64Value;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.pipeline.ContextFactory;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.tensorflow.framework.TensorProto;
import org.tensorflow.framework.TensorShapeProto;
import support.WordIndex;
import tensorflow.serving.Model;
import tensorflow.serving.Predict;
import tensorflow.serving.PredictionServiceGrpc;
import tensorflow.serving.PredictionServiceGrpc.PredictionServiceFutureStub;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.hazelcast.jet.datamodel.Tuple2.tuple2;

/**
 * Shows how to enrich a stream of movie reviews with classification using
 * a pre-trained TensorFlow model. Executes the TensorFlow model using gRPC
 * calls to a TensorFlow Model Server.
 */
public class ModelServerClassification {

    private static Pipeline buildPipeline(String dataPath, String serverAddress, IMap<Long, String> reviewsMap) {
        WordIndex wordIndex = new WordIndex(dataPath);
        ContextFactory<PredictionServiceFutureStub> tfServingContext = ContextFactory
                .withCreateFn(jet -> {
                    ManagedChannel channel = ManagedChannelBuilder.forTarget(serverAddress)
                                                                  .usePlaintext().build();
                    return PredictionServiceGrpc.newFutureStub(channel);
                })
                .withDestroyFn(stub -> ((ManagedChannel) stub.getChannel()).shutdownNow())
                .withLocalSharing()
                .withMaxPendingCallsPerProcessor(16);

        Pipeline p = Pipeline.create();
        p.drawFrom(Sources.map(reviewsMap))
         .map(Map.Entry::getValue)
         .mapUsingContextAsync(tfServingContext, (stub, review) -> {
             float[][] featuresTensorData = wordIndex.createTensorInput(review);
             TensorProto.Builder featuresTensorBuilder = TensorProto.newBuilder();
             for (float[] featuresTensorDatum : featuresTensorData) {
                 for (float v : featuresTensorDatum) {
                     featuresTensorBuilder.addFloatVal(v);
                 }
             }
             TensorShapeProto.Dim featuresDim1 =
                     TensorShapeProto.Dim.newBuilder().setSize(featuresTensorData.length).build();
             TensorShapeProto.Dim featuresDim2 =
                     TensorShapeProto.Dim.newBuilder().setSize(featuresTensorData[0].length).build();
             TensorShapeProto featuresShape =
                     TensorShapeProto.newBuilder().addDim(featuresDim1).addDim(featuresDim2).build();
             featuresTensorBuilder.setDtype(org.tensorflow.framework.DataType.DT_FLOAT)
                                  .setTensorShape(featuresShape);
             TensorProto featuresTensorProto = featuresTensorBuilder.build();

             // Generate gRPC request
             Int64Value version = Int64Value.newBuilder().setValue(1).build();
             Model.ModelSpec modelSpec =
                     Model.ModelSpec.newBuilder().setName("reviewSentiment").setVersion(version).build();
             Predict.PredictRequest request = Predict.PredictRequest.newBuilder()
                                                                    .setModelSpec(modelSpec)
                                                                    .putInputs("input_review", featuresTensorProto)
                                                                    .build();

             return toCompletableFuture(stub.predict(request))
                     .thenApply(response -> {
                         float classification = response
                                 .getOutputsOrThrow("dense_1/Sigmoid:0")
                                 .getFloatVal(0);
                         // emit the review along with the classification
                         return tuple2(review, classification);
                     });
         })
         .setLocalParallelism(1) // one worker is enough to drive they async calls
         .drainTo(Sinks.logger());
        return p;
    }

    public static void main(String[] args) {
        System.setProperty("hazelcast.logging.type", "log4j");

        if (args.length != 2) {
            System.out.println("Usage: ModelServerClassification <data path> <model server address>");
            System.exit(1);
        }
        String dataPath = args[0];
        String serverAddress = args[1];
        JetInstance instance = Jet.newJetInstance();
        try {
            IMap<Long, String> reviewsMap = instance.getMap("reviewsMap");
            SampleReviews.populateReviewsMap(reviewsMap);

            Pipeline p = buildPipeline(dataPath, serverAddress, reviewsMap);

            instance.newJob(p).join();
        } finally {
            instance.shutdown();
        }
    }

    /**
     * Adapt a {@link ListenableFuture} to java standard {@link
     * CompletableFuture}, which is used by Jet.
     */
    private static <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> lf) {
        CompletableFuture<T> f = new CompletableFuture<>();
        // note that we don't handle CompletableFuture.cancel()
        Futures.addCallback(lf, new FutureCallback<T>() {
            @Override
            public void onSuccess(@NullableDecl T result) {
                f.complete(result);
            }

            @Override
            public void onFailure(Throwable t) {
                f.completeExceptionally(t);
            }
        }, directExecutor());
        return f;
    }
}
