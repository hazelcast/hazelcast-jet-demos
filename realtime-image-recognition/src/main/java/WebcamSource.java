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
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.SourceBuilder.TimestampedSourceBuffer;
import com.hazelcast.jet.pipeline.StreamSource;

import java.awt.image.BufferedImage;

/**
 * A source that emits the frames captured from webcam video stream.
 * Also creates a GUI to show current captures.
 */
public class WebcamSource  {

    private final Webcam webcam;
    private final ImagePanel gui;
    private final long pollIntervalMillis;

    private long lastPoll;

    public WebcamSource(long pollIntervalMillis) {
        this.pollIntervalMillis = pollIntervalMillis;
        webcam = UtilWebcamCapture.openDefault(640, 480);
        gui = new ImagePanel();
        gui.setPreferredSize(webcam.getViewSize());
        ShowImages.showWindow(gui, "Webcam Input", true);
    }

    public void addToBufferFn(TimestampedSourceBuffer<BufferedImage> buffer) {
        long now = System.currentTimeMillis();
        if (now < (lastPoll + pollIntervalMillis)) {
            return;
        }
        lastPoll = now;
        BufferedImage image = webcam.getImage();
        gui.setImageRepaint(image);
        buffer.add(image, System.currentTimeMillis());
    }

    public static StreamSource<BufferedImage> webcam(long pollIntervalMillis) {
        return SourceBuilder.timestampedStream("webcam", ctx -> new WebcamSource(pollIntervalMillis))
                .fillBufferFn(WebcamSource::addToBufferFn)
                .destroyFn(WebcamSource::close)
                .build();
    }

    public void close() {
        if (webcam != null) {
            webcam.close();
        }
    }
}
