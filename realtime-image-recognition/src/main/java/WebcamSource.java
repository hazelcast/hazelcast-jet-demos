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
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamSource;
import java.awt.image.BufferedImage;
import java.io.Closeable;

import static com.hazelcast.jet.core.ProcessorMetaSupplier.forceTotalParallelismOne;

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
        return Sources.streamFromProcessor("webcam",
                forceTotalParallelismOne(new CloseableProcessorSupplier<>(WebcamSource::new))
        );
    }

    @Override
    public void close() {
        if (webcam != null) {
            webcam.close();
        }
    }
}
