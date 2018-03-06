package com.hazelcast.jet.demo;

import com.hazelcast.jet.IMapJet;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.demo.Aircraft.VerticalDirection;
import com.hazelcast.jet.demo.types.WakeTurbulanceCategory;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.SlidingWindowDef;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.pipeline.WindowDefinition;
import com.hazelcast.map.listener.EntryAddedListener;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.function.Consumer;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.modules.cPickle;

import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.aggregate.AggregateOperations.allOf;
import static com.hazelcast.jet.aggregate.AggregateOperations.linearTrend;
import static com.hazelcast.jet.aggregate.AggregateOperations.maxBy;
import static com.hazelcast.jet.aggregate.AggregateOperations.summingDouble;
import static com.hazelcast.jet.aggregate.AggregateOperations.toList;
import static com.hazelcast.jet.demo.Aircraft.VerticalDirection.ASCENDING;
import static com.hazelcast.jet.demo.Aircraft.VerticalDirection.CRUISE;
import static com.hazelcast.jet.demo.Aircraft.VerticalDirection.DESCENDING;
import static com.hazelcast.jet.demo.Aircraft.VerticalDirection.UNKNOWN;
import static com.hazelcast.jet.demo.Constants.heavyWTCClimbingAltitudeToNoiseDb;
import static com.hazelcast.jet.demo.Constants.heavyWTCDescendAltitudeToNoiseDb;
import static com.hazelcast.jet.demo.Constants.mediumWTCClimbingAltitudeToNoiseDb;
import static com.hazelcast.jet.demo.Constants.mediumWTCDescendAltitudeToNoiseDb;
import static com.hazelcast.jet.demo.Constants.typeToLTOCycyleC02Emission;
import static com.hazelcast.jet.demo.FlightDataSource.streamAircraft;
import static com.hazelcast.jet.demo.types.WakeTurbulanceCategory.HEAVY;
import static com.hazelcast.jet.demo.util.Util.inAtlanta;
import static com.hazelcast.jet.demo.util.Util.inFrankfurt;
import static com.hazelcast.jet.demo.util.Util.inIstanbul;
import static com.hazelcast.jet.demo.util.Util.inLondon;
import static com.hazelcast.jet.demo.util.Util.inNYC;
import static com.hazelcast.jet.demo.util.Util.inParis;
import static com.hazelcast.jet.demo.util.Util.inTokyo;
import static com.hazelcast.jet.function.DistributedComparator.comparingInt;
import static com.hazelcast.jet.impl.util.Util.uncheckCall;
import static com.hazelcast.jet.impl.util.Util.uncheckRun;
import static java.util.Collections.emptySortedMap;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The DAG used to model Flight Telemetry calculations can be seen below :
 *
 *                                                  ┌──────────────────┐
 *                                                  │Flight Data Source│
 *                                                  └─────────┬────────┘
 *                                                            │
 *                                                            v
 *                                           ┌─────────────────────────────────┐
 *                                           │Filter Aircraft  in Low Altitudes│
 *                                           └────────────────┬────────────────┘
 *                                                            │
 *                                                            v
 *                                                  ┌───────────────────┐
 *                                                  │Assign Airport Info│
 *                                                  └─────────┬─────────┘
 *                                                            │
 *                                                            v
 *                                                   ┌─────────────────┐
 *                                                   │Insert Watermarks│
 *                                                   └────────┬────────┘
 *                                                            │
 *                                                            v
 *                                          ┌───────────────────────────────────┐
 *                                          │Calculate Linear Trend of Altitudes│
 *                                          └─────────────────┬─────────────────┘
 *                                                            │
 *                                                            v
 *                                               ┌─────────────────────────┐
 *                                               │Assign Vertical Direction│
 *                                               └────┬────┬──┬───┬───┬────┘
 *                                                    │    │  │   │   │
 *                        ┌───────────────────────────┘    │  │   │   └──────────────────────────┐
 *                        │                                │  │   └─────────┐                    │
 *                        │                                │  └─────────┐   │                    │
 *                        v                                v            │   │                    │
 *             ┌────────────────────┐          ┌──────────────────────┐ │   │                    │
 *             │Enrich with C02 Info│          │Enrich with Noise Info│ │   │                    │
 *             └──┬─────────────────┘          └───────────┬──────────┘ │   │                    │
 *                │                                        │            │   │                    │
 *                │                          ┌─────────────┘            │   │                    │
 *                │                          │          ┌───────────────┘   │                    │
 *                v                          v          │                   v                    v
 *┌───────────────────────┐ ┌─────────────────────────┐ │ ┌───────────────────────────┐ ┌──────────────────────────┐
 *│Calculate Avg C02 Level│ │Calculate Max Noise Level│ │ │Filter Descending Aircraft │ │Filter Ascending Aircraft │
 *└──────────────┬────────┘ └────────────┬────────────┘ │ └─────────────┬─────────────┘ └─────────┬────────────────┘
 *               │                       │              │               │                         │
 *               │  ┌────────────────────┘              │               │                         │
 *               │  │  ┌────────────────────────────────┘               │                         │
 *               │  │  │                                                │                         │
 *               │  │  │                                                │                         │
 *               v  v  v                                                v                         v
 *           ┌─────────────┐                               ┌──────────────────────┐     ┌────────────────────────┐
 *           │Graphite Sink│                               │IMap Sink (landingMap)│     │IMap Sink (takingOffMap)│
 *           └─────────────┘                               └──────────────────────┘     └────────────────────────┘
 *
 */
public class FlightTelemetry {

    private static final String SOURCE_URL = "https://public-api.adsbexchange.com/VirtualRadar/AircraftList.json";
    private static final String TAKE_OFF_MAP = "takeOffMap";
    private static final String LANDING_MAP = "landingMap";

    static {
        System.setProperty("hazelcast.logging.type", "log4j");
    }

    public static void main(String[] args) {
        JetInstance jet = Jet.newJetInstance();

        Pipeline pipeline = buildPipeline();
        addListener(jet.getMap(TAKE_OFF_MAP), a -> System.out.println("New aircraft taking off: " + a));
        addListener(jet.getMap(LANDING_MAP), a -> System.out.println("New aircraft landing " + a));

        try {
            Job job = jet.newJob(pipeline);
            job.join();
        } finally {
            Jet.shutdownAll();
        }
    }

    private static Pipeline buildPipeline() {
        Pipeline p = Pipeline.create();

        SlidingWindowDef slidingWindow = WindowDefinition.sliding(60_000, 30_000);
        StreamStage<TimestampedEntry<Long, Aircraft>> flights = p
                .drawFrom(streamAircraft(SOURCE_URL, 10000))
                .addTimestamps(Aircraft::getPosTime, SECONDS.toMillis(15))
                .filter(a -> !a.isGnd() && a.getAlt() > 0 && a.getAlt() < 3000)
                .map(FlightTelemetry::assignAirport)
                .window(slidingWindow)
                .groupingKey(Aircraft::getId)
                .aggregate(
                        allOf(toList(), linearTrend(Aircraft::getPosTime, Aircraft::getAlt),
                                (events, coefficient) -> {
                                    Aircraft aircraft = events.get(events.size() - 1);
                                    aircraft.setVerticalDirection(getVerticalDirection(coefficient));
                                    return aircraft;
                                })
                ); // (timestamp, aircraft_id, aircraft_with_assigned_trend)

        StreamStage<TimestampedEntry<Long, Aircraft>> takingOffFlights = flights
                .filter(e -> e.getValue().verticalDirection == ASCENDING);

        takingOffFlights.drainTo(Sinks.map(TAKE_OFF_MAP)); // (aircraft_id, aircraft)

        StreamStage<TimestampedEntry<Long, Aircraft>> landingFlights = flights
                .filter(e -> e.getValue().verticalDirection == DESCENDING);

        landingFlights.drainTo(Sinks.map(LANDING_MAP)); // (aircraft_id, aircraft)

        StreamStage<TimestampedEntry<String, Integer>> maxNoise = flights
                .map(e -> entry(e.getValue(), getNoise(e.getValue()))) // (aircraft, noise)
                .window(slidingWindow)
                .groupingKey(e -> e.getKey().getAirport() + "_AVG_NOISE")
                .aggregate(maxBy(comparingInt(Entry::getValue)))
                .map(e -> new TimestampedEntry<>(e.getTimestamp(), e.getKey(), e.getValue().getValue()));
        // (airport, max_noise)

        StreamStage<TimestampedEntry<String, Double>> co2Emission = flights
                .map(e -> entry(e.getValue(), getCO2Emission(e.getValue()))) // (aircraft, co2_emission)
                .window(slidingWindow)
                .groupingKey(entry -> entry.getKey().getAirport() + "_C02_EMISSION")
                .aggregate(summingDouble(Entry::getValue));
        // (airport, total_co2)

        Sink<TimestampedEntry> graphiteSink = buildGraphiteSink("127.0.0.1", 2004);

        p.drainTo(graphiteSink, co2Emission, maxNoise, landingFlights, takingOffFlights);
        return p;
    }

    /**
     * Sink implementation which forwards the items it receives to the Graphite.
     * Graphite's Pickle Protocol is used for communication between Jet and Graphite.
     *
     * @param host Graphite host
     * @param port Graphite port
     */
    private static Sink<TimestampedEntry> buildGraphiteSink(String host, int port) {
        return Sinks.<BufferedOutputStream, TimestampedEntry>builder(instance -> uncheckCall(()
                -> new BufferedOutputStream(new Socket(host, port).getOutputStream())
        ))
                .onReceiveFn((bos, entry) -> uncheckRun(() -> {
                    GraphiteMetric metric = new GraphiteMetric();
                    metric.from(entry);

                    PyString payload = cPickle.dumps(metric.getAsList(), 2);
                    byte[] header = ByteBuffer.allocate(4).putInt(payload.__len__()).array();

                    bos.write(header);
                    bos.write(payload.toBytes());
                }))
                .flushFn((bos) -> uncheckRun(bos::flush))
                .destroyFn((bos) -> uncheckRun(bos::close))
                .build();
    }

    /**
     * Returns the average C02 emission on landing/take-offfor the aircraft
     *
     * @param aircraft
     * @return avg C02 for the aircraft
     */
    private static Double getCO2Emission(Aircraft aircraft) {
        return typeToLTOCycyleC02Emission.getOrDefault(aircraft.getType(), 0d);
    }

    /**
     * Returns the noise level at the current altitude of the aircraft
     *
     * @param aircraft
     * @return noise level of the aircraft
     */
    private static Integer getNoise(Aircraft aircraft) {
        Long altitude = aircraft.getAlt();
        SortedMap<Integer, Integer> lookupTable = getPhaseNoiseLookupTable(aircraft);
        if (lookupTable.isEmpty()) {
            return 0;
        }
        return lookupTable.tailMap(altitude.intValue()).values().iterator().next();
    }

    /**
     * Sets the airport field of the aircraft by looking at the coordinates of it
     *
     * @param aircraft
     */
    private static Aircraft assignAirport(Aircraft aircraft) {
        if (aircraft.getAlt() > 0 && !aircraft.isGnd()) {
            String airport = getAirport(aircraft.lon, aircraft.lat);
            if (airport == null) {
                return null;
            }
            aircraft.setAirport(airport);
        }
        return aircraft;
    }

    /**
     * Returns if the aircraft is in 80 mile radius area of the airport.
     *
     * @param lon longitude of the aircraft
     * @param lat latitude of the aircraft
     * @return name of the airport
     */
    private static String getAirport(float lon, float lat) {
        if (inLondon(lon, lat)) {
            return "London";
        } else if (inIstanbul(lon, lat)) {
            return "Istanbul";
        } else if (inFrankfurt(lon, lat)) {
            return "Frankfurt";
        } else if (inAtlanta(lon, lat)) {
            return "Atlanta";
        } else if (inParis(lon, lat)) {
            return "Paris";
        } else if (inTokyo(lon, lat)) {
            return "Tokyo";
        } else if (inNYC(lon, lat)) {
            return "New York";
        }
        // unknown city
        return null;
    }

    /**
     * Returns altitude to noise level lookup table for the aircraft based on its weight category
     *
     * @param aircraft
     * @return SortedMap contains altitude to noise level mappings.
     */
    private static SortedMap<Integer, Integer> getPhaseNoiseLookupTable(Aircraft aircraft) {
        VerticalDirection verticalDirection = aircraft.getVerticalDirection();
        WakeTurbulanceCategory wtc = aircraft.getWtc();
        if (ASCENDING.equals(verticalDirection)) {
            if (HEAVY.equals(wtc)) {
                return heavyWTCClimbingAltitudeToNoiseDb;
            } else {
                return mediumWTCClimbingAltitudeToNoiseDb;
            }
        } else if (DESCENDING.equals(verticalDirection)) {
            if (HEAVY.equals(wtc)) {
                return heavyWTCDescendAltitudeToNoiseDb;
            } else {
                return mediumWTCDescendAltitudeToNoiseDb;
            }
        }
        return emptySortedMap();
    }

    /**
     * Returns the vertical direction based on the linear trend coefficient of the altitude
     *
     * @param coefficient
     * @return VerticalDirection enum value
     */
    private static VerticalDirection getVerticalDirection(double coefficient) {
        if (coefficient == Double.NaN) {
            return UNKNOWN;
        }
        if (coefficient > 0) {
            return ASCENDING;
        } else if (coefficient == 0) {
            return CRUISE;
        } else {
            return DESCENDING;
        }
    }

    /**
     * Attaches a listener to {@link IMapJet} which passes added items to the specified consumer
     *
     * @param map      map instance which the listener will be added
     * @param consumer aircraft consumer that the added items will be passed on.
     */
    private static void addListener(IMapJet<Long, Aircraft> map, Consumer<Aircraft> consumer) {
        map.addEntryListener((EntryAddedListener<Long, Aircraft>) event ->
                consumer.accept(event.getValue()), true);
    }

    /**
     * A data transfer object for Graphite
     */
    private static class GraphiteMetric {
        PyString metricName;
        PyInteger timestamp;
        PyFloat metricValue;

        private GraphiteMetric() {
        }

        private void fromAirCraftEntry(TimestampedEntry<Long, Aircraft> aircraftEntry) {
            Aircraft aircraft = aircraftEntry.getValue();
            metricName = new PyString(replaceWhiteSpace(aircraft.getAirport()) + "." + aircraft.verticalDirection);
            timestamp = new PyInteger(getEpochSecond(aircraft.getPosTime()));
            metricValue = new PyFloat(1);
        }

        private void fromMaxNoiseEntry(TimestampedEntry<String, Integer> entry) {
            metricName = new PyString(replaceWhiteSpace(entry.getKey()));
            timestamp = new PyInteger(getEpochSecond(entry.getTimestamp()));
            metricValue = new PyFloat(entry.getValue());
        }

        private void fromTotalC02Entry(TimestampedEntry<String, Double> entry) {
            metricName = new PyString(replaceWhiteSpace(entry.getKey()));
            timestamp = new PyInteger(getEpochSecond(entry.getTimestamp()));
            metricValue = new PyFloat(entry.getValue());
        }

        void from(TimestampedEntry entry) {
            if (entry.getKey() instanceof Long) {
                TimestampedEntry<Long, Aircraft> aircraftEntry = entry;
                fromAirCraftEntry(aircraftEntry);
            } else {
                if (entry.getValue() instanceof Double) {
                    fromTotalC02Entry(entry);
                } else {
                    fromMaxNoiseEntry(entry);
                }
            }
        }

        PyList getAsList() {
            PyList list = new PyList();
            PyTuple metric = new PyTuple(metricName, new PyTuple(timestamp, metricValue));
            list.add(metric);
            return list;
        }

        private int getEpochSecond(long millis) {
            return (int) Instant.ofEpochMilli(millis).getEpochSecond();
        }

        private String replaceWhiteSpace(String string) {
            return string.replace(" ", "_");
        }
    }
}
