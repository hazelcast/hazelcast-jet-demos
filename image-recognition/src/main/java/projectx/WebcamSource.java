package projectx;

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
import com.hazelcast.jet.function.DistributedFunction;
import com.hazelcast.nio.Address;
import com.hazelcast.partition.strategy.StringPartitioningStrategy;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * date: 1/23/18
 * author: emindemirci
 */
public class WebcamSource extends AbstractProcessor implements Closeable {

    private Traverser<BufferedImage> traverser;
    private Webcam webcam;
    private ImagePanel gui;

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
            BufferedImage image = webcam.getImage();
            gui.setImageRepaint(image);
            traverser = Traverser.over(image);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
        webcam.close();
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
                    return new CloseableProcessorSupplier<>(WebcamSource::new);
                }
                // return empty producer on all other nodes
                return c -> singletonList(Processors.noopP().get());
            };
        }
    }
}
