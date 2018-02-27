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

import boofcv.abst.scene.ImageClassifier.Score;
import boofcv.gui.ImageClassificationPanel;
import boofcv.gui.image.ShowImages;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.TimestampedItem;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.Sinks;
import java.awt.*;
import java.sql.Timestamp;
import java.util.Map.Entry;
import javax.swing.*;

import static com.hazelcast.jet.core.ProcessorMetaSupplier.forceTotalParallelismOne;
import static com.hazelcast.jet.core.ProcessorSupplier.of;
import static java.util.Collections.singletonList;

/**
 * A GUI which will show the frames with the maximum classification scores.
 */
public class GUISink extends AbstractProcessor {

    private ImageClassificationPanel panel;

    @Override
    protected void init(Context context) {
        panel = new ImageClassificationPanel();
        ShowImages.showWindow(panel, "Results", true);
    }

    @Override
    protected boolean tryProcess(int ordinal, Object item) {
        TimestampedItem<Entry<SerializableBufferedImage, Entry<String, Double>>> timestampedItem =
                (TimestampedItem<Entry<SerializableBufferedImage, Entry<String, Double>>>) item;
        Entry<SerializableBufferedImage, Entry<String, Double>> entry = timestampedItem.item();
        SerializableBufferedImage image = entry.getKey();
        Entry<String, Double> category = entry.getValue();
        Score score = new Score();
        score.set(category.getValue(), 0);
        String timestampString = new Timestamp(timestampedItem.timestamp()).toString();
        panel.addImage(image.getImage(), timestampString, singletonList(score), singletonList(category.getKey()));
        scrollToBottomAndRepaint();
        return true;
    }

    private void scrollToBottomAndRepaint() {
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) component;
                JList list = (JList) scrollPane.getViewport().getView();
                int size = list.getModel().getSize();
                list.setSelectedIndex(Math.max(size - 2, list.getLastVisibleIndex()));
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
                panel.repaint(scrollPane.getBounds());
            }
        }
    }

    @Override
    public boolean isCooperative() {
        return false;
    }

    public static Sink<TimestampedItem> sink() {
        return Sinks.fromProcessor("guiSink", forceTotalParallelismOne(of(GUISink::new)));
    }
}
