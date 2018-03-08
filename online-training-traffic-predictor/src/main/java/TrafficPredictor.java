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

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.pipeline.ContextFactories;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamStage;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.aggregate.AggregateOperations.linearTrend;
import static com.hazelcast.jet.impl.util.Util.toLocalDateTime;
import static com.hazelcast.jet.pipeline.Sources.files;
import static com.hazelcast.jet.pipeline.WindowDefinition.sliding;
import static java.lang.Integer.parseInt;
import static java.time.ZoneId.systemDefault;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * 
 * This application reads the traffic data from file. Each input record contains timestamp, location and
 * respective car count. After ingestion, the records are routed to two separate computations.
 * First computation uses the car count to train the model. Jet collects data from last two hours
 * and computes the trend using the linear regression algorithm. This trend is updated every 15 minutes
 * and stored into an IMap. Sliding Windows are used to select two hour blocks from the time series.
 * Second computation combines current car count with the trend from previous week to predict car count
 * in next two hours. Prediction results are stored in a file in predictions/ directory.
 *
 * The training data is obtained from:
 * https://catalog.data.gov/dataset/nys-thruway-origin-and-destination-points-for-all-vehicles-15-minute-intervals-2016-q1
 * <p>
 * We've grouped the data by date and entry location and sorted them.
 *
 * The DAG used to model traffic predictor can be seen below :
 *
 *                           ┌────────────────────────┐
 *                           │Read historic car counts│
 *                           │       from file        │
 *                           └──┬──────────────┬──────┘
 *                              │              │
 *                              │              └─────────┐
 *                              v                        │
 *                      ┌──────────────┐                 │
 *                      │Add timestamps│                 │
 *                      └───────┬──────┘                 │
 *                              │                        │
 *                              v                        │
 *                     ┌─────────────────┐               │
 *                     │Group by location│               │
 *                     └─────────┬───────┘               │
 *                               │                       │
 *                               v                       │
 *                  ┌─────────────────────────┐          │
 *                  │Calculate linear trend of│          │
 *                  │ counts in 2hour windows │          │
 *                  │     slides by 15mins    │          │
 *                  └────────────┬────────────┘          │
 *                               │                       │
 *                 ┌─────────────┘                       │
 *                 │                                     │
 *                 v                                     v
 *   ┌──────────────────────────┐ ┌────────────────────────────────────────────┐
 *   │Format linear trend output│ │Make predictions based on the historic trend│
 *   └────────────────┬─────────┘ └──────────────────────────┬─────────────────┘
 *                    │                                      │
 *                    v                                      v
 *   ┌────────────────────────────────┐ ┌────────────────────────────────────────┐
 *   │Write results to an IMap(trends)│ │Write results to a file(targetDirectory)│
 *   └────────────────────────────────┘ └────────────────────────────────────────┘
 */
public class TrafficPredictor {

    private static final long GRANULARITY_STEP_MS = MINUTES.toMillis(15);
    private static final int NUM_PREDICTIONS = 8;

    static {
        System.setProperty("hazelcast.multicast.group", "224.18.19.21");
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Missing command-line arguments: <input file> <output directory>");
            System.exit(1);
        }

        Path sourceFile = Paths.get(args[0]).toAbsolutePath();
        final String targetDirectory = args[1];
        if (!Files.isReadable(sourceFile)) {
            System.err.println("Source file does not exist or is not readable (" + sourceFile + ")");
            System.exit(1);
        }

        JetInstance instance = Jet.newJetInstance();
        Pipeline pipeline = buildPipeline(sourceFile, targetDirectory);
        try {
            instance.newJob(pipeline).join();
        } finally {
            Jet.shutdownAll();
        }
    }
    /**
     * Builds and returns the Pipeline which represents the actual computation.
     */
    private static Pipeline buildPipeline(Path sourceFile, String targetDirectory) {
        Pipeline pipeline = Pipeline.create();

        // Calculate car counts from the file.
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

        // Calculate linear trends of car counts and writes them into an IMap
        // in 2 hour windows sliding by 15 minutes.
        carCounts
                .groupingKey(CarCount::getLocation)
                .window(sliding(MINUTES.toMillis(120), MINUTES.toMillis(15)))
                .aggregate(linearTrend(CarCount::getTime, CarCount::getCount))
                .map((TimestampedEntry<String, Double> e) ->
                        entry(new TrendKey(e.getKey(), e.getTimestamp()), e.getValue()))
                .drainTo(Sinks.map("trends"));

        // Makes predictions using the trends calculated above from an IMap and writes them to a file
        carCounts
                .mapUsingContext(ContextFactories.<TrendKey, Double>iMapContext("trends"),
                        (trendMap, cc) -> {
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
                            return new Prediction(cc.location, cc.time + GRANULARITY_STEP_MS, counts);
                        })
                .drainTo(Sinks.files(targetDirectory));
        return pipeline;
    }

    /**
     * Composite key object of time and location which used on IMap
     */
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

    /**
     * DTO for predictions
     */
    private static class Prediction implements Serializable {
        private final String location;
        // time of the first prediction
        private final long time;
        // predictions, one for each minute
        private final int[] predictedCounts;

        private Prediction(String location, long time, int[] predictedCounts) {
            this.location = location;
            this.time = time;
            this.predictedCounts = predictedCounts;
        }

        public String getLocation() {
            return location;
        }

        public long getTime() {
            return time;
        }

        public int[] getPredictedCounts() {
            return predictedCounts;
        }

        @Override
        public String toString() {
            return "Prediction{" +
                    "location='" + location + '\'' +
                    ", time=" + toLocalDateTime(time) + " (" + time + ")" +
                    ", predictedCounts=" + Arrays.toString(predictedCounts) +
                    '}';
        }
    }

    /**
     * DTO for car counts of a location on a specific time
     */
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
