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
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.TimestampKind;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.WindowDefinition;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;

import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.core.Edge.between;
import static com.hazelcast.jet.core.Edge.from;
import static com.hazelcast.jet.core.WatermarkEmissionPolicy.emitByFrame;
import static com.hazelcast.jet.core.WatermarkGenerationParams.wmGenParams;
import static com.hazelcast.jet.core.WatermarkPolicies.withFixedLag;
import static com.hazelcast.jet.core.WindowDefinition.slidingWindowDef;
import static com.hazelcast.jet.core.processor.Processors.aggregateToSlidingWindowP;
import static com.hazelcast.jet.core.processor.Processors.insertWatermarksP;
import static com.hazelcast.jet.core.processor.Processors.mapP;
import static com.hazelcast.jet.core.processor.SinkProcessors.writeFileP;
import static com.hazelcast.jet.core.processor.SinkProcessors.writeMapP;
import static com.hazelcast.jet.core.processor.SourceProcessors.readFilesP;
import static com.hazelcast.jet.impl.util.Util.toLocalDateTime;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * The training data is obtained from:
 * https://catalog.data.gov/dataset/nys-thruway-origin-and-destination-points-for-all-vehicles-15-minute-intervals-2016-q1
 * <p>
 * We've grouped the data by date and entry location and sorted them.
 */
public class Main {

    private static final long GRANULARITY_STEP_MS = MINUTES.toMillis(15);

    static {
        System.setProperty("hazelcast.multicast.group", "224.18.19.21");
    }

    public static void main(String[] args) {
        Path sourceFile = Paths.get(args[0]).toAbsolutePath();
        final String targetDirectory = "predictions";

        JetInstance instance = Jet.newJetInstance();
        DAG dag = buildDAG(sourceFile, targetDirectory);
        try {
            instance.newJob(dag).join();
        } finally {
            Jet.shutdownAll();
        }
    }

    private static DAG buildDAG(Path sourceFile, String targetDirectory) {
        DAG dag = new DAG();
        WindowDefinition windowDef = slidingWindowDef(MINUTES.toMillis(120), MINUTES.toMillis(15));

        Vertex source = dag.newVertex("source",
                readFilesP(
                        sourceFile.getParent().toString(),
                        StandardCharsets.UTF_8,
                        sourceFile.getFileName().toString(),
                        (filename, line) -> {
                            String[] split = line.split(",");
                            long time = LocalDateTime.parse(split[0]).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                            return new CarCount(split[1], time, Integer.parseInt(split[2]));

                        })).localParallelism(1);
        Vertex insertWm = dag.newVertex("insertWm", insertWatermarksP(wmGenParams(
                CarCount::getTime, withFixedLag(MINUTES.toMillis(300)), emitByFrame(windowDef), -1
        ))).localParallelism(1);
        Vertex calcTrend = dag.newVertex("calcTrend", aggregateToSlidingWindowP(
                CarCount::getLocation,
                CarCount::getTime,
                TimestampKind.EVENT,
                windowDef,
                AggregateOperations.linearTrend(CarCount::getTime, CarCount::getCount)
        ));
        Vertex mapTrend = dag.newVertex("mapTrend", mapP((TimestampedEntry<String, Double> en) ->
                entry(new TrendKey(en.getKey(), en.getTimestamp()), en.getValue())));
        Vertex storeTrend = dag.newVertex("storeTrend", writeMapP("trends"));

        // 2nd path: use the trends
        Vertex useTrend = dag.newVertex("useTrend", PredictionP::new);
        Vertex storePredictions = dag.newVertex("storePredictions", writeFileP(targetDirectory))
                                     .localParallelism(1);

        dag.edge(between(source, insertWm))
           .edge(between(insertWm, calcTrend)
                   .partitioned(CarCount::getLocation))
           .edge(between(calcTrend, mapTrend))
           .edge(between(mapTrend, storeTrend))
           // 2nd path
           .edge(from(source, 1).to(useTrend))
           .edge(between(useTrend, storePredictions));
        return dag;
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
            CarCount cc = (CarCount) item;
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
            return tryEmit(prediction);
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
