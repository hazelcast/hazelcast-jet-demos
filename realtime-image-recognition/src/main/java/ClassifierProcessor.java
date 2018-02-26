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

import boofcv.abst.scene.ImageClassifier;
import boofcv.abst.scene.ImageClassifier.Score;
import boofcv.deepboof.ImageClassifierVggCifar10;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.impl.pipeline.JetEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.hazelcast.jet.Traverser.over;
import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.impl.pipeline.JetEventImpl.jetEvent;

/**
 * Processor implementation which does the actual classification of the images
 * by using the pre-trained model.
 */
public class ClassifierProcessor extends AbstractProcessor {


    private ImageClassifier<Planar<GrayF32>> classifier;
    private List<String> categories;
    private String modelPath;

    public ClassifierProcessor(String modelPath) {
        this.modelPath = modelPath;
    }

    @Override
    protected void init(Context context) throws Exception {
        classifier = new ImageClassifierVggCifar10();
        classifier.loadModel(new File(modelPath));
        categories = classifier.getCategories();
    }

    @Override
    protected boolean tryProcess(int ordinal, Object item) {
        JetEvent<SerializableBufferedImage> event = (JetEvent<SerializableBufferedImage>) item;
        SerializableBufferedImage serializableBufferedImage = event.payload();
        BufferedImage image = serializableBufferedImage.getImage();

        Planar<GrayF32> planar = new Planar<>(GrayF32.class, image.getWidth(), image.getHeight(), 3);
        ConvertBufferedImage.convertFromPlanar(image, planar, true, GrayF32.class);

        classifier.classify(planar);
        List<Score> results = classifier.getAllResults();
        List<Entry<String, Double>> categoryWithScores = results.stream().map(score -> entry(categories.get(score.category), score.score)).collect(Collectors.toList());
        Entry<String, Double> maxScoredCategory = categoryWithScores
                .stream()
                .max(Comparator.comparing(Entry::getValue))
                .get();
        return emitFromTraverser(over(jetEvent(entry(serializableBufferedImage, maxScoredCategory), event.timestamp())));
    }

}
