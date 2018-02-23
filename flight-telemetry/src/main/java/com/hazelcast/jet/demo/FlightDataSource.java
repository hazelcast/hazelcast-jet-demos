package com.hazelcast.jet.demo;


import com.hazelcast.com.eclipsesource.json.Json;
import com.hazelcast.com.eclipsesource.json.JsonArray;
import com.hazelcast.com.eclipsesource.json.JsonObject;
import com.hazelcast.com.eclipsesource.json.JsonValue;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.util.ExceptionUtil;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

import static com.hazelcast.jet.Traversers.traverseIterable;
import static com.hazelcast.jet.core.ProcessorMetaSupplier.dontParallelize;
import static com.hazelcast.jet.impl.util.Util.uncheckCall;
import static com.hazelcast.jet.pipeline.Sources.streamFromProcessor;
import static com.hazelcast.util.StringUtil.isNullOrEmpty;
import static java.util.stream.Collectors.toList;

/**
 * Polls the <a href="https://www.adsbexchange.com">ADS-B Exchange</a> HTTP API
 * for flight data. The API will be polled every {@code pollIntervalMillis} milliseconds.
 * <p>
 * After a successful poll, this source filters out aircrafts which are missing registration number
 * and position timestamp. It will also records the latest position timestamp of the aircrafts so if
 * there are no update for an aircraft it will not be emitted from this source.
 */
public class FlightDataSource extends AbstractProcessor {

    private final URL url;
    private final long intervalMillis;
    private final Map<Long, Long> idToTimestamp = new HashMap<>();

    private Traverser<Aircraft> traverser;
    private long lastPoll;

    public FlightDataSource(String url, long pollIntervalMillis) {
        try {
            this.url = new URL(url);
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
                traverser = uncheckCall(this::poll);
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

    private Traverser<Aircraft> poll() throws Exception {
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.addRequestProperty("User-Agent", "Mozilla / 5.0 (Windows NT 6.1; WOW64) AppleWebKit / 537.36 (KHTML, like Gecko) Chrome / 40.0.2214.91 Safari / 537.36");
        con.getResponseCode();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        con.disconnect();

        JsonValue value = Json.parse(response.toString());
        JsonObject object = value.asObject();
        JsonArray acList = object.get("acList").asArray();

        List<Aircraft> newEvents =
                acList.values().stream()
                      .map(this::parseAc)
                      .filter(a -> !isNullOrEmpty(a.reg)) // there should be a reg number
                      .filter(a -> a.posTime > 0) // there should be a timestamp
                      .filter(a -> {
                          // only emit updated newEvents
                          Long newTs = a.posTime;
                          if (newTs <= 0) {
                              return false;
                          }
                          Long oldTs = idToTimestamp.get(a.id);
                          if (oldTs != null && newTs <= oldTs) {
                              return false;
                          }
                          idToTimestamp.put(a.id, newTs);
                          return true;
                      }).collect(toList());
        getLogger().info("Polled " + acList.size() + " aircraft, " + newEvents.size() + " new locations.");
        return traverseIterable(newEvents);
    }

    private Aircraft parseAc(JsonValue ac) {
        Aircraft aircraft = new Aircraft();
        aircraft.fromJson(ac.asObject());
        return aircraft;
    }

    public static ProcessorMetaSupplier streamAircraftP(String url, long intervalMillis) {
        return dontParallelize(() -> new FlightDataSource(url, intervalMillis));
    }

    public static StreamSource<Aircraft> streamAircraft(String url, long intervalMillis) {
        return streamFromProcessor("streamAircraft", streamAircraftP(url, intervalMillis));
    }

}
