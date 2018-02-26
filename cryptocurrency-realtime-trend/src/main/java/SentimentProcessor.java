import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ResettableSingletonTraverser;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import edu.stanford.nlp.util.CoreMap;

import javax.annotation.Nonnull;
import java.util.List;

import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;

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
