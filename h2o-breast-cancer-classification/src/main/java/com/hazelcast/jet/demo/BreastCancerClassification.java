package com.hazelcast.jet.demo;

import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.accumulator.MutableReference;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.demo.model.BreastCancerDiagnostic;
import com.hazelcast.jet.demo.model.TumorType;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactory;
import com.hazelcast.jet.pipeline.Sinks;
import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.prediction.BinomialModelPrediction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.hazelcast.jet.pipeline.Sources.filesBuilder;

public class BreastCancerClassification {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Missing command-line arguments: <model-file-path> <validation-input-data>");
            System.exit(1);
        }

        Path modelFile = Paths.get(args[0]).toAbsolutePath();
        Path inputFile = Paths.get(args[1]).toAbsolutePath();
        validateFileReadable(modelFile);
        validateFileReadable(inputFile);

        System.setProperty("hazelcast.logging.type", "log4j");

        JetInstance jet = Jet.newJetInstance();

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName("h2o Breast Cancer Classification");
        jobConfig.attachFile(modelFile.toString(), "model");
        jobConfig.addJar(modelFile.toString());
        Job job = jet.newJob(buildPipeline(inputFile), jobConfig);

        try {
            job.join();
        } finally {
            jet.shutdown();
        }
    }

    private static void validateFileReadable(Path path) {
        if (!Files.isReadable(path)) {
            System.err.println("File does not exist or is not readable (" + path + ")");
            System.exit(1);
        }
    }

    /**
     * Builds and returns the Pipeline which represents the actual computation.
     */
    private static Pipeline buildPipeline(Path sourceFile) {
        Pipeline pipeline = Pipeline.create();

        BatchStage<String> fileSource = pipeline.readFrom(filesBuilder(sourceFile.getParent().toString())
                .glob(sourceFile.getFileName().toString())
                .build())
                .setName("Read from CSV input file");

        fileSource.apply(skipHeaderLine())
                  .apply(mapToPOJO())
                  .apply(applyPredictionFromModelFile())
                  .writeTo(Sinks.logger()).setName("Write to standard out");
        return pipeline;
    }

    private static FunctionEx<BatchStage<BreastCancerDiagnostic>, BatchStage<String>> applyPredictionFromModelFile() {
        return stage -> stage.mapUsingService(modelContext(), (modelWrapper, diagnostic) -> {
            BinomialModelPrediction p = modelWrapper.predictBinomial(diagnostic.asRow());
            TumorType predictedTumorType = TumorType.fromString(p.label);
            if (diagnostic.getDiagnosis() == predictedTumorType) {
                return "Match: " + "Actual: " + String.format("%1$-" + 15 + "s", diagnostic.getDiagnosis()) + "Prediction: "
                        + formatPrediction(p, predictedTumorType);
            } else {
                return "Miss:  " + "Actual: " + String.format("%1$-" + 15 + "s", diagnostic.getDiagnosis()) + "Prediction: "
                        + formatPrediction(p, predictedTumorType);
            }
        }).setName("Apply H2O classification from loaded MOJO");
    }

    private static FunctionEx<BatchStage<String>, BatchStage<BreastCancerDiagnostic>> mapToPOJO() {
        return stage -> stage.map(line -> {
            String[] split = line.split(",");
            return new BreastCancerDiagnostic(split);
        }).setName("Map CSV text line to Java object");
    }

    private static FunctionEx<BatchStage<String>, BatchStage<String>> skipHeaderLine() {
        return stage -> stage.filterStateful(() -> new MutableReference<>(false), (headerSeen, line) -> {
            if (!headerSeen.get()) {
                headerSeen.set(true);
                return false;
            }
            return true;
        }).setName("Skip CSV header row");
    }

    private static ServiceFactory<EasyPredictModelWrapper, EasyPredictModelWrapper> modelContext() {
        return ServiceFactory.withCreateContextFn(context -> {
            String model = context.attachedFile("model").toString();
            return new EasyPredictModelWrapper(MojoModel.load(model));
        }).withCreateServiceFn((context, easyPredictModelWrapper) -> easyPredictModelWrapper);
    }

    private static String formatPrediction(BinomialModelPrediction p, TumorType predictedTumorType) {
        StringBuilder builder = new StringBuilder(String.format("%1$-" + 20 + "s", predictedTumorType));
        for (int i = 0; i < p.classProbabilities.length; i++) {
            builder.append(String.format("%1$-" + 25 + "s", p.classProbabilities[i]));
        }
        return builder.toString();
    }

}
