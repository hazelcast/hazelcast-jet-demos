package com.hazelcast.jet.demo.core;

import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ResettableSingletonTraverser;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.demo.common.SentimentAnalyzer;

import javax.annotation.Nonnull;

import static com.hazelcast.jet.datamodel.Tuple3.tuple3;
import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;

/**
 * Calculates sentiment score for a coin.
 */
public class SentimentProcessor extends AbstractProcessor {

    private final SentimentAnalyzer analyzer = new SentimentAnalyzer();
    private final ResettableSingletonTraverser<Tuple3<Long, String, Double>> traverser = new
            ResettableSingletonTraverser<>();
    private final FlatMapper<Tuple3<Long, String, String>, Tuple3<Long, String, Double>> mapper =
            flatMapper(e -> {
                double score = analyzer.getSentimentScore(e.f2());
                if (!isNaN(score) && !isInfinite(score)) {
                    traverser.accept(tuple3(e.f0(), e.f1(), score));
                }
                return traverser;
            });

    @Override
    protected boolean tryProcess(int ordinal, @Nonnull Object item) {
        return mapper.tryProcess(((Tuple3<Long, String, String>) item));
    }
}
