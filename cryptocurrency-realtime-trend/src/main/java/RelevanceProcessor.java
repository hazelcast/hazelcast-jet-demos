import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.TimestampedEntry;

import javax.annotation.Nonnull;

public class RelevanceProcessor extends AbstractProcessor {


    @Override
    protected boolean tryProcess(int ordinal, @Nonnull Object item) throws Exception {
        TimestampedEntry<String, String> content = (TimestampedEntry<String, String>) item;

        for (String cointype : CoinDefs.coinMap.keySet()) {
            for (String keyword : CoinDefs.coinMap.get(cointype)) {
                if (content.getKey().contains(keyword)) {
                    TimestampedEntry<String, String> entry = new TimestampedEntry<>(content.getTimestamp(), cointype, content.getKey());
//                    System.out.println("content = " + content.getKey() + " " + cointype);
                    tryEmit(entry);
                }
            }
        }
        return true;
    }
}
