import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public class RelevanceProcessor extends AbstractProcessor {

    private Traverser<TimestampedEntry> traverser;

    @Override
    protected boolean tryProcess(int ordinal, @Nonnull Object item) throws Exception {
        if (traverser == null) {
            TimestampedEntry<String, String> content = (TimestampedEntry<String, String>) item;
            List<TimestampedEntry> relevant = new ArrayList<>();
            for (String cointype : CoinDefs.coinMap.keySet()) {
                for (String keyword : CoinDefs.coinMap.get(cointype)) {
                    if (content.getKey().contains(keyword)) {
                        TimestampedEntry<String, String> entry = new TimestampedEntry<>(content.getTimestamp(), cointype, content.getKey());
                        relevant.add(entry);
                    }
                }
            }
            traverser = Traversers.traverseIterable(relevant);
        }
        if (emitFromTraverser(traverser)) {
            traverser = null;
            return true;
        }
        return false;
    }
}
