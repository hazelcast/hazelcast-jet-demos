package com.hazelcast.jet.demo.core;

import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ResettableSingletonTraverser;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.demo.common.SentimentAnalyzer;
import edu.stanford.nlp.util.CoreMap;
import java.util.List;
import javax.annotation.Nonnull;

import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;

/**
 * Calculates sentiment score for a coin.
 */
public class SentimentProcessor extends AbstractProcessor {

    private final SentimentAnalyzer analyzer = new SentimentAnalyzer();
    private final ResettableSingletonTraverser<TimestampedEntry<String, Double>> traverser = new
            ResettableSingletonTraverser<>();
    private final FlatMapper<TimestampedEntry<String, String>, TimestampedEntry<String, Double>> mapper =
            flatMapper(e -> {
                List<CoreMap> annotations = analyzer.getAnnotations(e.getValue());
                double sentimentType = analyzer.getSentimentClass(annotations);
                double sentimentScore = analyzer.getScore(annotations, sentimentType);

                double score = sentimentType * sentimentScore;
                if (isNaN(score) || isInfinite(score)) {
                    return Traversers.empty();
                }
                traverser.accept(new TimestampedEntry<>(e.getTimestamp(), e.getKey(), score));
                return traverser;
            });

    @Override
    protected boolean tryProcess(int ordinal, @Nonnull Object item) {
        return mapper.tryProcess(((TimestampedEntry<String, String>) item));
    }
}
