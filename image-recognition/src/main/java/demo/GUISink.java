package demo;


import boofcv.abst.scene.ImageClassifier.Score;
import boofcv.gui.ImageClassificationPanel;
import boofcv.gui.image.ShowImages;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.hazelcast.jet.core.processor.Processors;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.function.DistributedFunction;
import com.hazelcast.nio.Address;
import com.hazelcast.partition.strategy.StringPartitioningStrategy;
import java.util.List;
import java.util.Map.Entry;

import static java.util.Collections.singletonList;

/**
 * A GUI which will show the frames with the maximum classification scores.
 */
public class GUISink extends AbstractProcessor {

    private ImageClassificationPanel panel;

    @Override
    protected void init(Context context) throws Exception {
        panel = new ImageClassificationPanel();
        ShowImages.showWindow(panel, "Results", true);
    }

    @Override
    protected boolean tryProcess(int ordinal, Object item) throws Exception {
        TimestampedEntry<String, Entry<SerializableBufferedImage, Entry<String, Double>>> timestampedEntry = (TimestampedEntry<String, Entry<SerializableBufferedImage, Entry<String, Double>>>) item;
        Entry<SerializableBufferedImage, Entry<String, Double>> imageEntry = timestampedEntry.getValue();
        SerializableBufferedImage image = imageEntry.getKey();
        Entry<String, Double> category = imageEntry.getValue();
        Score score = new Score();
        score.set(category.getValue(), 0);
        panel.addImage(image.getImage(), timestampedEntry.getKey() + timestampedEntry.getTimestamp(), singletonList(score), singletonList(category.getKey()));
        return true;
    }

    @Override
    public boolean isCooperative() {
        return false;
    }

    public static ProcessorMetaSupplier sink() {
        return new MetaSupplier();
    }


    private static class MetaSupplier implements ProcessorMetaSupplier {

        private Address ownerAddress;

        @Override
        public int preferredLocalParallelism() {
            return 1;
        }

        @Override
        public void init(Context context) {
            String partitionKey = StringPartitioningStrategy.getPartitionKey("gui");
            ownerAddress = context.jetInstance().getHazelcastInstance().getPartitionService()
                                  .getPartition(partitionKey).getOwner().getAddress();
        }

        @Override
        public DistributedFunction<Address, ProcessorSupplier> get(List<Address> addresses) {
            return address -> {
                if (address.equals(ownerAddress)) {
                    return ProcessorSupplier.of(GUISink::new);
                }
                // return empty producer on all other nodes
                return c -> singletonList(Processors.noopP().get());
            };
        }
    }

}
