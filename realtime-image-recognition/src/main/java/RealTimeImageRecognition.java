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

import boofcv.abst.scene.ImageClassifier.Score;
import boofcv.deepboof.ImageClassifierVggCifar10;
import boofcv.gui.ImageClassificationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.datamodel.TimestampedItem;
import com.hazelcast.jet.pipeline.ContextFactory;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.Sinks;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.swing.*;

import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.aggregate.AggregateOperations.maxBy;
import static com.hazelcast.jet.function.DistributedComparator.comparingDouble;
import static com.hazelcast.jet.pipeline.WindowDefinition.tumbling;
import static com.hazelcast.util.ExceptionUtil.rethrow;
import static java.util.Collections.singletonList;

/**
 * An application which uses webcam frame stream as the input and classifies the
 * frames with a model pre-trained with CIFAR-10 dataset.
 * <p>
 * Second-worth of frames will be aggregated to find the classification with
 * maximum score that will be sent to a GUI sink to be shown on the screen.
 *
 * The DAG used to model image recognition calculations can be seen below :
 *
 *              ┌───────────────────┐
 *              │Webcam Video Source│
 *              └─────────┬─────────┘
 *                        │
 *                        v
 *                ┌──────────────┐
 *                │Add Timestamps│
 *                └────────┬─────┘
 *                         │
 *                         v
 *        ┌────────────────────────────────┐
 *        │Classify Images with pre-trained│
 *        │     machine learning model     │
 *        └───────────────┬────────────────┘
 *                        │
 *                        v
 *            ┌───────────────────────┐
 *            │Calculate maximum score│
 *            │    in 1 sec windows   │
 *            └───────────┬───────────┘
 *                        │
 *                        v
 *              ┌───────────────────┐
 *              │Show results on GUI│
 *              └───────────────────┘

 */
public class RealTimeImageRecognition {

    static {
        System.setProperty("hazelcast.logging.type", "slf4j");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Missing command-line argument: <model path>");
            System.exit(1);
        }

        Path modelPath = Paths.get(args[0]).toAbsolutePath();
        if (!Files.isDirectory(modelPath)) {
            System.err.println("Model path does not exist (" + modelPath + ")");
            System.exit(1);
        }

        Pipeline pipeline = buildPipeline(modelPath.toString());

        JetInstance jet = Jet.newJetInstance();
        try {
            jet.newJob(pipeline).join();
        } finally {
            Jet.shutdownAll();
        }
    }

    /**
     * Builds and returns the Pipeline which represents the actual computation.
     */
    private static Pipeline buildPipeline(String modelPath) {
        Pipeline pipeline = Pipeline.create();
        pipeline.drawFrom(WebcamSource.webcam())
                .addTimestamps()
                .mapUsingContext(classifierContext(modelPath), RealTimeImageRecognition::classifyWithModel)
                .window(tumbling(1000))
                .aggregate(maxBy(comparingDouble(e -> e.getValue().getValue())))
                .drainTo(buildGUISink());
        return pipeline;
    }

    /**
     * A GUI Sink which will show the frames with the maximum classification scores.
     */
    private static Sink<TimestampedItem<Entry<SerializableBufferedImage, Entry<String, Double>>>> buildGUISink() {
        return Sinks.builder("GUI", (instance) -> createPanel())
                .receiveFn(RealTimeImageRecognition::addItemToPanel)
                .build();
    }

    /**
     * Adds an new image to the result GUI panel
     */
    private static void addItemToPanel(ImageClassificationPanel panel,
                                       TimestampedItem<Entry<SerializableBufferedImage, Entry<String, Double>>> item) {
        SerializableBufferedImage image = item.item().getKey();
        Entry<String, Double> category = item.item().getValue();
        Score score = new Score();
        score.set(category.getValue(), 0);
        String timestampString = new Timestamp(item.timestamp()).toString();
        panel.addImage(image.getImage(), timestampString, singletonList(score), singletonList(category.getKey()));
        scrollToBottomAndRepaint(panel);
    }

    /**
     * Scrolls the GUI panel to the bottom to show latest result
     * whenever a new image added to the GUI panel
     */
    private static void scrollToBottomAndRepaint(ImageClassificationPanel panel) {
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) component;
                JList list = (JList) scrollPane.getViewport().getView();
                int size = list.getModel().getSize();
                list.setSelectedIndex(Math.max(size - 2, list.getLastVisibleIndex()));
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
                panel.repaint(scrollPane.getBounds());
            }
        }
    }

    /**
     * Creates and returns image result GUI panel
     */
    private static ImageClassificationPanel createPanel() {
        ImageClassificationPanel panel = new ImageClassificationPanel();
        ShowImages.showWindow(panel, "Results", true);
        return panel;
    }

    /**
     * The actual classification of the images by using the pre-trained model.
     */
    private static Entry<SerializableBufferedImage, Entry<String, Double>> classifyWithModel(
            ImageClassifierVggCifar10 classifier, SerializableBufferedImage serializableBufferedImage) {
        BufferedImage image = serializableBufferedImage.getImage();

        Planar<GrayF32> planar = new Planar<>(GrayF32.class, image.getWidth(), image.getHeight(), 3);
        ConvertBufferedImage.convertFromPlanar(image, planar, true, GrayF32.class);

        classifier.classify(planar);
        List<Score> results = classifier.getAllResults();
        List<Entry<String, Double>> categoryWithScores = results.stream().map(score -> entry(classifier.getCategories().get(score.category), score.score)).collect(Collectors.toList());
        Entry<String, Double> maxScoredCategory = categoryWithScores
                .stream()
                .max(Comparator.comparing(Entry::getValue))
                .get();
        return entry(serializableBufferedImage, maxScoredCategory);
    }

    /**
     * Loads the pre-trained model from the specified path
     *
     * @param modelPath path of the model
     */
    private static ContextFactory<ImageClassifierVggCifar10> classifierContext(String modelPath) {
        return ContextFactory.withCreateFn(jet -> {
            ImageClassifierVggCifar10 classifier = new ImageClassifierVggCifar10();
            try {
                classifier.loadModel(new File(modelPath));
            } catch (IOException e) {
                throw rethrow(e);
            }
            return classifier;
        });
    }

}
