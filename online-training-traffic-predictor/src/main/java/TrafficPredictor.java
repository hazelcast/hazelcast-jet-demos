/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
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

import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.impl.pipeline.JetEvent;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamStage;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.aggregate.AggregateOperations.linearTrend;
import static com.hazelcast.jet.impl.pipeline.JetEventImpl.jetEvent;
import static com.hazelcast.jet.impl.util.Util.toLocalDateTime;
import static com.hazelcast.jet.pipeline.Sources.files;
import static com.hazelcast.jet.pipeline.WindowDefinition.sliding;
import static java.lang.Integer.parseInt;
import static java.time.ZoneId.systemDefault;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * The training data is obtained from:
 * https://catalog.data.gov/dataset/nys-thruway-origin-and-destination-points-for-all-vehicles-15-minute-intervals-2016-q1
 * <p>
 * We've grouped the data by date and entry location and sorted them.
 */
public class TrafficPredictor {

    private static final long GRANULARITY_STEP_MS = MINUTES.toMillis(15);

    static {
        System.setProperty("hazelcast.multicast.group", "224.18.19.21");
    }

    public static void main(String[] args) {
        Path sourceFile = Paths.get(args[0]).toAbsolutePath();
        final String targetDirectory = "predictions";

        JetInstance instance = Jet.newJetInstance();
        Pipeline pipeline = buildPipeline(sourceFile, targetDirectory);
        try {
            instance.newJob(pipeline).join();
        } finally {
            Jet.shutdownAll();
        }
    }

    private static Pipeline buildPipeline(Path sourceFile, String targetDirectory) {
        Pipeline pipeline = Pipeline.create();
        StreamStage<CarCount> carCounts = pipeline.drawFrom(
                files(
                        sourceFile.getParent().toString(),
                        StandardCharsets.UTF_8,
                        sourceFile.getFileName().toString(), (filename, line) -> {
                            String[] split = line.split(",");
                            long time = LocalDateTime.parse(split[0])
                                                     .atZone(systemDefault())
                                                     .toInstant()
                                                     .toEpochMilli();
                            return new CarCount(split[1], time, parseInt(split[2]));
                        }
                )
        ).addTimestamps(CarCount::getTime, MINUTES.toMillis(300));

        carCounts
                .groupingKey(CarCount::getLocation)
                .window(sliding(MINUTES.toMillis(120), MINUTES.toMillis(15)))
                .aggregate(linearTrend(CarCount::getTime, CarCount::getCount))
                .map((TimestampedEntry<String, Double> e) ->
                        entry(new TrendKey(e.getKey(), e.getTimestamp()), e.getValue()))
                .drainTo(Sinks.map("trends"));

        carCounts.customTransform("useTrend", PredictionP::new)
                 .drainTo(Sinks.files(targetDirectory));
        return pipeline;
    }

    private static class PredictionP extends AbstractProcessor {
        private static final int NUM_PREDICTIONS = 8;

        private IMap<TrendKey, Double> trendMap;

        @Override
        protected void init(Context context) {
            trendMap = context.jetInstance().getMap("trends");
        }

        @Override
        protected boolean tryProcess(int ordinal, Object item) {
            JetEvent<CarCount> event = ((JetEvent<CarCount>) item);
            CarCount cc = event.payload();
            int[] counts = new int[NUM_PREDICTIONS];
            double trend = 0.0;
            for (int i = 0; i < NUM_PREDICTIONS; i++) {
                Double newTrend = trendMap.get(new TrendKey(cc.location, cc.time - DAYS.toMillis(7)));
                if (newTrend != null) {
                    trend = newTrend;
                }
                double prediction = cc.count + i * GRANULARITY_STEP_MS * trend;
                counts[i] = (int) Math.round(prediction);
            }
            Prediction prediction = new Prediction(cc.location, cc.time + GRANULARITY_STEP_MS, counts);
            return tryEmit(jetEvent(prediction, event.timestamp()));
        }
    }

    private static class TrendKey implements Serializable {
        private final String location;
        private final long time;

        private TrendKey(String location, long time) {
            this.location = location;
            this.time = time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TrendKey trendKey = (TrendKey) o;
            return time == trendKey.time &&
                    Objects.equals(location, trendKey.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, time);
        }

        @Override
        public String toString() {
            return "TrendKey{" +
                    "location='" + location + '\'' +
                    ", time=" + toLocalDateTime(time) +
                    '}';
        }
    }

    private static class Prediction implements Serializable {
        private final String location;
        // time of the first prediction
        private final long time;
        // predictions, one for each minute
        private final int[] counts;

        private Prediction(String location, long time, int[] counts) {
            this.location = location;
            this.time = time;
            this.counts = counts;
        }

        public String getLocation() {
            return location;
        }

        public long getTime() {
            return time;
        }

        public int[] getCounts() {
            return counts;
        }

        @Override
        public String toString() {
            return "Prediction{" +
                    "location='" + location + '\'' +
                    ", time=" + toLocalDateTime(time) + " (" + time + ")" +
                    ", counts=" + Arrays.toString(counts) +
                    '}';
        }
    }

    private static class CarCount {
        private final String location;
        private final long time;
        private final int count;

        private CarCount(String location, long time, int count) {
            this.location = location;
            this.time = time;
            this.count = count;
        }

        public String getLocation() {
            return location;
        }

        public long getTime() {
            return time;
        }

        public int getCount() {
            return count;
        }

        @Override
        public String toString() {
            return "CarCount{" +
                    "location='" + location + '\'' +
                    ", time=" + toLocalDateTime(time) +
                    ", count=" + count +
                    '}';
        }
    }
}
