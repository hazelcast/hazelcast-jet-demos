package com.hazelcast.jet.demo;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Processor;
import com.hazelcast.jet.core.TimestampKind;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.Watermark;
import com.hazelcast.jet.core.WindowDefinition;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.demo.Aircraft.VerticalDirection;
import com.hazelcast.jet.demo.types.WakeTurbulanceCategory;
import com.hazelcast.jet.function.DistributedSupplier;
import com.hazelcast.jet.function.DistributedToDoubleFunction;
import com.hazelcast.jet.function.DistributedToIntFunction;
import com.hazelcast.jet.stream.IStreamMap;
import com.hazelcast.map.listener.EntryAddedListener;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.aggregate.AggregateOperations.allOf;
import static com.hazelcast.jet.aggregate.AggregateOperations.linearTrend;
import static com.hazelcast.jet.aggregate.AggregateOperations.maxBy;
import static com.hazelcast.jet.aggregate.AggregateOperations.summingDouble;
import static com.hazelcast.jet.aggregate.AggregateOperations.toList;
import static com.hazelcast.jet.core.Edge.between;
import static com.hazelcast.jet.core.Edge.from;
import static com.hazelcast.jet.core.ProcessorSupplier.of;
import static com.hazelcast.jet.core.WatermarkEmissionPolicy.emitByFrame;
import static com.hazelcast.jet.core.WatermarkGenerationParams.wmGenParams;
import static com.hazelcast.jet.core.WatermarkPolicies.limitingLagAndDelay;
import static com.hazelcast.jet.core.processor.DiagnosticProcessors.peekInputP;
import static com.hazelcast.jet.core.processor.DiagnosticProcessors.peekOutputP;
import static com.hazelcast.jet.core.processor.Processors.aggregateToSlidingWindowP;
import static com.hazelcast.jet.core.processor.Processors.filterP;
import static com.hazelcast.jet.core.processor.Processors.insertWatermarksP;
import static com.hazelcast.jet.core.processor.Processors.mapP;
import static com.hazelcast.jet.core.processor.SinkProcessors.writeMapP;
import static com.hazelcast.jet.demo.Aircraft.VerticalDirection.ASCENDING;
import static com.hazelcast.jet.demo.Aircraft.VerticalDirection.CRUISE;
import static com.hazelcast.jet.demo.Aircraft.VerticalDirection.DESCENDING;
import static com.hazelcast.jet.demo.Aircraft.VerticalDirection.UNKNOWN;
import static com.hazelcast.jet.demo.types.WakeTurbulanceCategory.HEAVY;
import static com.hazelcast.jet.demo.util.Util.inAtlanta;
import static com.hazelcast.jet.demo.util.Util.inFrankfurt;
import static com.hazelcast.jet.demo.util.Util.inIstanbul;
import static com.hazelcast.jet.demo.util.Util.inLondon;
import static com.hazelcast.jet.demo.util.Util.inNYC;
import static com.hazelcast.jet.demo.util.Util.inParis;
import static com.hazelcast.jet.demo.util.Util.inTokyo;
import static com.hazelcast.jet.function.DistributedComparator.comparingInt;

/**
 *
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

        DAG dag = buildDAG();

        addListener(jet.getMap(TAKE_OFF_MAP), a -> System.out.println("New aircraft taking off: " + a));
        addListener(jet.getMap(LANDING_MAP), a -> System.out.println("New aircraft landing " + a));

        try {
            Job job = jet.newJob(dag);
            job.join();
        } finally {
            Jet.shutdownAll();
        }
    }

    private static DAG buildDAG() {
        DAG dag = new DAG();

        // initialize the source vertex which polls the API for every 10s
        Vertex source = dag.newVertex("flightDataSource", FlightDataSource.streamAircraftP(SOURCE_URL, 10000));

        // we are interested only in planes above the ground and altitude less than 3000
        Vertex filterLowAltitude = dag.newVertex("filterLowAltitude", filterP(
                (Aircraft ac) -> !ac.isGnd() && ac.getAlt() > 0 && ac.getAlt() < 3000)
        ).localParallelism(1);

        Vertex assignAirport = dag.newVertex("assignAirport", mapP(FlightTelemetry::assignAirport)).localParallelism(1);

        WindowDefinition wDefTrend = WindowDefinition.slidingWindowDef(60_000, 30_000);
        DistributedSupplier<Processor> insertWMP = insertWatermarksP(wmGenParams(
                Aircraft::getPosTime,
                limitingLagAndDelay(TimeUnit.MINUTES.toMillis(15), 10_000),
                emitByFrame(wDefTrend),
                60000L
        ));
        Vertex insertWm = dag.newVertex("insertWm", peekOutputP(
                Object::toString,
                o -> o instanceof Watermark,
                insertWMP)).localParallelism(1);

        // aggregation: calculate altitude trend
        Vertex aggregateTrend = dag.newVertex("aggregateTrend", aggregateToSlidingWindowP(
                Aircraft::getId,
                Aircraft::getPosTime,
                TimestampKind.EVENT,
                wDefTrend,
                allOf(toList(), linearTrend(Aircraft::getPosTime, Aircraft::getAlt))
        ));

        Vertex addVerticalDirection = dag.newVertex("addVerticalDirection", mapP(FlightTelemetry::assignDirection));

        Vertex filterAscending = dag.newVertex("filterAscending", filterP(
                (Entry<Long, Aircraft> e) -> e.getValue().verticalDirection == ASCENDING));

        Vertex filterDescending = dag.newVertex("filterDescending", filterP(
                (Entry<Long, Aircraft> e) -> e.getValue().verticalDirection == DESCENDING));

        Vertex enrichWithNoiseInfo = dag.newVertex("enrich with noise info", mapP(
                (Entry<Long, Aircraft> entry) -> {
                    Aircraft aircraft = entry.getValue();
                    Long altitude = aircraft.getAlt();
                    SortedMap<Integer, Integer> lookupTable = getPhaseNoiseLookupTable(aircraft);
                    if (lookupTable.isEmpty()) {
                        return null;
                    }
                    Integer currentDb = lookupTable.tailMap(altitude.intValue()).values().iterator().next();
                    return entry(aircraft, currentDb);
                })
        );

        Vertex maxNoiseLevel = dag.newVertex("max noise level", aggregateToSlidingWindowP(
                (Entry<Aircraft, Integer> entry) -> entry.getKey().getAirport() + "_AVG_NOISE",
                (Entry<Aircraft, Integer> entry) -> entry.getKey().getPosTime(),
                TimestampKind.EVENT,
                wDefTrend,
                maxBy(comparingInt((DistributedToIntFunction<Entry<Aircraft, Integer>>) Entry::getValue))
                )
        );

        Vertex enrichWithC02Emission = dag.newVertex("enrich with C02 emission info", mapP(
                (Entry<Long, Aircraft> entry) -> {
                    Aircraft aircraft = entry.getValue();
                    Double ltoC02EmissionInKg = Constants.typeToLTOCycyleC02Emission.getOrDefault(aircraft.getType(), 0d);
                    return entry(aircraft, ltoC02EmissionInKg);
                }
        ));

        Vertex averagePollution = dag.newVertex("pollution", peekInputP(aggregateToSlidingWindowP(
                (Entry<Aircraft, Double> entry) -> entry.getKey().getAirport() + "_C02_EMISSION",
                (Entry<Aircraft, Double> entry) -> entry.getKey().getPosTime(),
                TimestampKind.EVENT,
                wDefTrend,
                summingDouble(
                        (DistributedToDoubleFunction<Entry<Aircraft, Double>>) Entry::getValue
                )
        )));

        Vertex takeOffMapSink = dag.newVertex("takeOffMapSink", writeMapP(TAKE_OFF_MAP));
        Vertex landingMapSink = dag.newVertex("landingMapSink", writeMapP(LANDING_MAP));

        Vertex graphiteSink = dag.newVertex("graphite sink",
                of(() -> new GraphiteSink("127.0.0.1", 2004))
        );

        dag.edge(between(source, filterLowAltitude));

        // flights over interested cities and less then altitude 3000ft.
        dag.edge(between(filterLowAltitude, assignAirport));

        // identify flight phase (LANDING/TAKE-OFF)
        dag
                .edge(between(assignAirport, insertWm))
                .edge(between(insertWm, aggregateTrend)
                        .distributed()
                        .partitioned(Aircraft::getId))
                .edge(between(aggregateTrend, addVerticalDirection));

        // max noise path
        dag
                .edge(from(addVerticalDirection, 0).to(enrichWithNoiseInfo))
                .edge(between(enrichWithNoiseInfo, maxNoiseLevel)
                        .distributed()
                        .partitioned((Entry<Aircraft, Integer> entry) -> entry.getKey().getAirport()))
                .edge(between(maxNoiseLevel, graphiteSink));


        // C02 emission calculation path
        dag
                .edge(from(addVerticalDirection, 1).to(enrichWithC02Emission))
                .edge(between(enrichWithC02Emission, averagePollution)
                        .distributed()
                        .allToOne())
                .edge(from(averagePollution).to(graphiteSink, 1));

        // taking off planes to IMap
        dag.edge(from(addVerticalDirection, 2).to(filterAscending))
           .edge(from(filterAscending).to(takeOffMapSink));

        // landing planes to IMap
        dag.edge(from(addVerticalDirection, 3).to(filterDescending))
           .edge(from(filterDescending).to(landingMapSink));

        dag.edge(from(addVerticalDirection, 4).to(graphiteSink, 2));

        return dag;
    }

    private static Aircraft assignAirport(Aircraft ac) {
        if (ac.getAlt() > 0 && !ac.isGnd()) {
            String airport = getAirport(ac.lon, ac.lat);
            if (airport == null) {
                return null;
            }
            ac.setAirport(airport);
        }
        return ac;
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
                return Constants.heavyWTCClimbingAltitudeToNoiseDb;
            } else {
                return Constants.mediumWTCClimbingAltitudeToNoiseDb;
            }
        } else if (DESCENDING.equals(verticalDirection)) {
            if (HEAVY.equals(wtc)) {
                return Constants.heavyWTCDescendAltitudeToNoiseDb;
            } else {
                return Constants.mediumWTCDescendAltitudeToNoiseDb;
            }
        }
        return Collections.emptySortedMap();
    }

    /**
     * Assigns the vertical direction based on the linear trend coefficient of the altitude of the aircraft.
     *
     * @param entry contains the altitude and results from the linear trend calculation.
     * @return an entry whose key is the altitude and value is the aircraft object which contains the vertical direction.
     */
    private static Entry<Long, Aircraft> assignDirection(TimestampedEntry<Long, Tuple2<List<Aircraft>,Double>> entry) {
        // unpack the results
        List<Aircraft> aircraftList = entry.getValue().f0();
        Aircraft aircraft = aircraftList.get(0);
        double coefficient = entry.getValue().f1();
        aircraft.setVerticalDirection(getVerticalDirection(coefficient));
        return entry(entry.getKey(), aircraft);
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

    private static void addListener(IStreamMap<Long, Aircraft> map, Consumer<Aircraft> consumer) {
        map.addEntryListener((EntryAddedListener<Long, Aircraft>) event -> consumer.accept(event.getValue()), true);
    }

}
