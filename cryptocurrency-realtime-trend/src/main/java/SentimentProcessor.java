import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import edu.stanford.nlp.util.CoreMap;

import javax.annotation.Nonnull;
import java.util.List;

public class SentimentProcessor extends AbstractProcessor {

    private SentimentAnalyzer analyzer;

    @Override
    protected void init(@Nonnull Context context) throws Exception {
        super.init(context);
        analyzer = new SentimentAnalyzer();
    }

    @Override
    protected boolean tryProcess(int ordinal, @Nonnull Object item) throws Exception {
        TimestampedEntry<String, String> entry = (TimestampedEntry<String, String>) item;

        String coinType = entry.getKey();
        String tweetText = entry.getValue();

        List<CoreMap> annotations = analyzer.getAnnotations(tweetText);

        double sentimentType = analyzer.getSentimentClass(annotations);
        double sentimentScore = analyzer.getScore(annotations, sentimentType);

        double score = sentimentType * sentimentScore;

        TimestampedEntry<String, Double> result = new TimestampedEntry<>(entry.getTimestamp(), coinType, score);
//        System.out.println(tweetText + " " + result.getKey() + " " + score);
        System.out.println(result.getKey() + " " + score);
        return tryEmit(result);
    }
}
