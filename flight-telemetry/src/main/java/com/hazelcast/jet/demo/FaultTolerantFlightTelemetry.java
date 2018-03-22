package com.hazelcast.jet.demo;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;

import java.io.IOException;

import static com.hazelcast.jet.demo.FlightTelemetry.buildPipeline;
import static com.hazelcast.jet.demo.ReplayableFlightDataSource.buildJobConfig;

/**
 * Starts fault tolerant version of the flight telemetry demo application.
 * In this mode, we replace the source processor with a reliable map journal
 * from which the flight telemetry pipeline receives aircraft events. The map
 * journal source implementation is replayable. Additionally, the Graphite
 * sink is idempotent. Therefore, the flight telemetry job offers end-to-end
 * exactly-once processing guarantee.
 * <p/>
 * The fault tolerant mode requires a bit of ceremony to run as follows:
 * <ul>
 * <li>
 *     First, run the application with "-Dtype=server" to start a Jet node. You
 *     can start multiple Jet nodes with this approach and form a Jet cluster.
 * </li>
 * <li>
 *     After you have at least one Jet node running, start another instance of
 *     the application with "-Dtype=source". This is because we need to
 *     populate the source map with aircraft events fetched from the ADS-B
 *     Exchange API.
 * </li>
 * <li>
 *     Once you perform these steps, you have a running Jet cluster and a
 *     client application populating the aircraft events map source. However,
 *     the flight telemetry job is not running yet. Each Jet node starts with
 *     a command line console to submit and track the job. Just type "submit"
 *     in one of the consoles, the job starts running. You can type "help" for
 *     other commands.
 * </li>
 * </ul>
 */
public class FaultTolerantFlightTelemetry {

    private static final GroupConfig GROUP_CONFIG = new GroupConfig("flight", "demo");

    static {
        System.setProperty("hazelcast.logging.type", "log4j");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String type = System.getProperty("type", "").trim().toLowerCase();
        switch (type) {
            case "server":
                startJetServer();
                break;
            case "client":
                startJetClient();
                break;
            case "source":
                startSource();
                break;
            default:
                System.err.println("Invalid type: " + type + "available options: type=server|client|source");
        }
    }

    private static void startJetServer() {
        JetConfig config = ReplayableFlightDataSource.buildJetConfig();
        config.getHazelcastConfig().setGroupConfig(GROUP_CONFIG);
        JetInstance jet = Jet.newJetInstance(config);

        try {
            JobConsole c = new JobConsole(jet, buildPipeline(jet, ReplayableFlightDataSource.buildSource()), buildJobConfig());
            c.run();
        } finally {
            jet.getHazelcastInstance().getLifecycleService().terminate();
        }
    }

    private static void startJetClient() {
        ClientConfig config = new ClientConfig();
        config.setGroupConfig(GROUP_CONFIG);
        JetInstance jet = Jet.newJetClient(config);

        try {
            JobConsole c = new JobConsole(jet, buildPipeline(jet, ReplayableFlightDataSource.buildSource()), buildJobConfig());
            c.run();
        } finally {
            jet.getHazelcastInstance().getLifecycleService().terminate();
        }
    }

    private static void startSource() throws IOException, InterruptedException {
        ClientConfig config = new ClientConfig();
        config.setGroupConfig(GROUP_CONFIG);
        JetInstance jet = Jet.newJetClient(config);

        PollAircraft fetcher = new PollAircraft(jet);
        fetcher.run();
    }

}
