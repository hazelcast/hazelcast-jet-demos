/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.nio.Address;
import com.hazelcast.partition.strategy.StringPartitioningStrategy;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * A source that emits the frames captured from webcam stream.
 * Also creates an GUI to show current captures.
 */
public class WebcamSource extends AbstractProcessor implements Closeable {

    private Traverser<SerializableBufferedImage> traverser;
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
                traverser = Traverser.over(new SerializableBufferedImage(image));
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


    public static StreamSource<SerializableBufferedImage> webcam() {
        return Sources.streamFromProcessor("webcam", new MetaSupplier());
    }

    public static ProcessorMetaSupplier metaSupplier() {
        return new MetaSupplier();
    }

    @Override
    public void close() {
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
