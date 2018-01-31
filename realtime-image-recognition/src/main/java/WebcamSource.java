import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import com.github.sarxos.webcam.Webcam;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.CloseableProcessorSupplier;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.hazelcast.jet.core.processor.Processors;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.function.DistributedFunction;
import com.hazelcast.nio.Address;
import com.hazelcast.partition.strategy.StringPartitioningStrategy;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * A source that emits the frames captured from webcam stream.
 * Also creates an GUI to show current captures.
 */
public class WebcamSource extends AbstractProcessor implements Closeable {

    private Traverser<TimestampedEntry> traverser;
    private Webcam webcam;
    private ImagePanel gui;
    private long lastPoll;
    private long intervalMillis = 500;

    @Override
    protected void init(Context context) throws Exception {
        super.init(context);
        webcam = UtilWebcamCapture.openDefault(640, 480);
        gui = new ImagePanel();
        gui.setPreferredSize(webcam.getViewSize());
        ShowImages.showWindow(gui, "Webcam Input", true);
    }

    @Override
    public boolean complete() {
        if (traverser == null) {
            long now = System.currentTimeMillis();
            if (now > lastPoll + intervalMillis) {
                lastPoll = now;
                BufferedImage image = webcam.getImage();
                gui.setImageRepaint(image);
                traverser = Traverser.over(new TimestampedEntry<>(now, new SerializableBufferedImage(image), null));
            } else {
                return false;
            }
        }
        if (emitFromTraverser(traverser)) {
            traverser = null;
        }
        return false;
    }

    @Override
    public boolean isCooperative() {
        return false;
    }


    public static ProcessorMetaSupplier webcam() {
        return new MetaSupplier();
    }

    @Override
    public void close() throws IOException {
        if (webcam != null) {
            webcam.close();
        }
    }


    private static class MetaSupplier implements ProcessorMetaSupplier {

        private Address ownerAddress;

        @Override
        public int preferredLocalParallelism() {
            return 1;
        }

        @Override
        public void init(Context context) {
            String partitionKey = StringPartitioningStrategy.getPartitionKey("webcam");
            ownerAddress = context.jetInstance().getHazelcastInstance().getPartitionService()
                                  .getPartition(partitionKey).getOwner().getAddress();
        }

        @Override
        public DistributedFunction<Address, ProcessorSupplier> get(List<Address> addresses) {
            return address -> {
                if (address.equals(ownerAddress)) {
                    return new CloseableProcessorSupplier<>(WebcamSource::new);
                }
                // return empty producer on all other nodes
                return c -> singletonList(Processors.noopP().get());
            };
        }
    }
}
