package com.hazelcast.jet.demo;


import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.util.ExceptionUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hazelcast.jet.Traversers.traverseIterable;
import static com.hazelcast.jet.core.ProcessorMetaSupplier.forceTotalParallelismOne;
import static com.hazelcast.jet.core.ProcessorSupplier.of;
import static com.hazelcast.jet.demo.PollAircraft.poll;
import static com.hazelcast.jet.impl.util.Util.uncheckCall;
import static com.hazelcast.jet.pipeline.Sources.streamFromProcessor;

/**
 * Polls the <a href="https://www.adsbexchange.com">ADS-B Exchange</a> HTTP API
 * for flight data.
 * The API will be polled every {@code pollIntervalMillis} milliseconds.
 * <p/>
 * This is an unreliable source implementation. It directly polls aircraft data
 * from the API and emits aircraft events to downstream.
 *
 * @see PollAircraft
 */
public class FlightDataSource extends AbstractProcessor {

    public static final String SOURCE_URL = "https://public-api.adsbexchange.com/VirtualRadar/AircraftList.json";

    private final URL url;
    private final long intervalMillis;
    private final Map<Long, Long> idToTimestamp = new HashMap<>();

    private Traverser<Aircraft> traverser;
    private long lastPoll;

    public FlightDataSource(long pollIntervalMillis) {
        try {
            this.url = new URL(SOURCE_URL);
        } catch (MalformedURLException e) {
            throw ExceptionUtil.rethrow(e);
        }
        this.intervalMillis = pollIntervalMillis;
    }

    @Override
    public boolean complete() {
        if (traverser == null) {
            long now = System.currentTimeMillis();
            if (now > lastPoll + intervalMillis) {
                lastPoll = now;
                List<Aircraft> aircrafts = uncheckCall(() -> poll(url, idToTimestamp));
                traverser = traverseIterable(aircrafts);
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

    public static ProcessorMetaSupplier streamAircraftP(long intervalMillis) {
        return forceTotalParallelismOne(of(() -> new FlightDataSource(intervalMillis)));
    }

    public static StreamSource<Aircraft> streamAircraft(long intervalMillis) {
        return streamFromProcessor("streamAircraft", streamAircraftP(intervalMillis));
    }

}
