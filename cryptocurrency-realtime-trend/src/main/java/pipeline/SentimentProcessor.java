package pipeline;

import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ResettableSingletonTraverser;
import com.hazelcast.jet.impl.pipeline.JetEvent;
import common.SentimentAnalyzer;
import edu.stanford.nlp.util.CoreMap;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map.Entry;

import static com.hazelcast.jet.impl.pipeline.JetEvent.jetEvent;
import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;

public class SentimentProcessor extends AbstractProcessor {

    private final SentimentAnalyzer analyzer = new SentimentAnalyzer();
    private final ResettableSingletonTraverser<JetEvent<Entry<String, Double>>> traverser = new
            ResettableSingletonTraverser<>();
    private final FlatMapper<JetEvent<Entry<String, String>>, JetEvent<Entry<String, Double>>> mapper =
            flatMapper(event -> {
                Entry<String, String> entry = event.payload();
                List<CoreMap> annotations = analyzer.getAnnotations(entry.getValue());
                double sentimentType = analyzer.getSentimentClass(annotations);
                double sentimentScore = analyzer.getScore(annotations, sentimentType);


                double score = sentimentType * sentimentScore;
                if (isNaN(score) || isInfinite(score)) {
                    return Traversers.empty();
                }
                traverser.accept(jetEvent(Util.entry(entry.getKey(), score), event.timestamp()));
                return traverser;
            });

    @Override
    protected boolean tryProcess(int ordinal, @Nonnull Object item) {
        return mapper.tryProcess(((JetEvent<Entry<String, String>>) item));
    }
}
