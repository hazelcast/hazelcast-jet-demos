package com.hazelcast.jet.demo;

import com.hazelcast.com.eclipsesource.json.Json;
import com.hazelcast.com.eclipsesource.json.JsonArray;
import com.hazelcast.com.eclipsesource.json.JsonObject;
import com.hazelcast.com.eclipsesource.json.JsonValue;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.JetInstance;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hazelcast.util.StringUtil.isNullOrEmpty;
import static java.util.stream.Collectors.toList;

/**
 * Polls the <a href="https://www.adsbexchange.com">ADS-B Exchange</a> HTTP API
 * for flight data. Polled aircraft events are put into
 * {@link ReplayableFlightDataSource#SOURCE_MAP}
 * <p>
 * After a successful poll, this source filters out aircrafts which are missing
 * registration number and position timestamp. It will also records the latest
 * position timestamp of the aircrafts so if there are no update for an
 * aircraft it will be ignored.
 */
public class PollAircraft {

    private static final Logger LOGGER = Logger.getLogger(PollAircraft.class);
    private static final String SOURCE_URL = "https://public-api.adsbexchange.com/VirtualRadar/AircraftList.json";

    private final Map<Long, Long> idToTimestamp = new HashMap<>();
    private final URL url;
    private final IMap<Long, Aircraft> sink;

    PollAircraft(JetInstance instance) throws MalformedURLException {
        this.url = new URL(SOURCE_URL);
        this.sink = instance.getMap(ReplayableFlightDataSource.SOURCE_MAP);
    }

    public void run() throws IOException, InterruptedException {
        while (true) {
            List<Aircraft> aircrafts = poll(url, idToTimestamp);
            aircrafts.forEach(a -> sink.put(a.getId(), a));

            LOGGER.info("Polled " + aircrafts.size() + " new locations. map size: " + sink.size());

            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        }
    }

    static List<Aircraft> poll(URL url, Map<Long, Long> idToTimestamp) throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.addRequestProperty("User-Agent", "Mozilla / 5.0 (Windows NT 6.1; WOW64) AppleWebKit / 537.36 "
                + "(KHTML, like Gecko) Chrome / 40.0.2214.91 Safari / 537.36");
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

        return acList.values().stream()
                .map(PollAircraft::parseAc)
                .filter(a -> !isNullOrEmpty(a.getReg())) // there should be a reg number
                .filter(a -> a.getPosTime() > 0) // there should be a timestamp
                .filter(a -> { // only emit updated newEvents
                    Long newTs = a.getPosTime();
                    if (newTs <= 0) {
                        return false;
                    }
                    Long oldTs = idToTimestamp.get(a.getId());
                    if (oldTs != null && newTs <= oldTs) {
                        return false;
                    }
                    idToTimestamp.put(a.getId(), newTs);
                    return true;
                }).collect(toList());
    }

    private static Aircraft parseAc(JsonValue ac) {
        Aircraft aircraft = new Aircraft();
        aircraft.fromJson(ac.asObject());
        return aircraft;
    }

}
