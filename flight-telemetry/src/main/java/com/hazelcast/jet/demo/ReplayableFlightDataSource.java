package com.hazelcast.jet.demo;

import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.function.DistributedFunction;
import com.hazelcast.jet.function.DistributedPredicate;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.map.journal.EventJournalMapEvent;

import static com.hazelcast.jet.pipeline.JournalInitialPosition.START_FROM_OLDEST;
import static com.hazelcast.jet.pipeline.Sources.mapJournal;

/**
 * Builds the map journal that serves the aircraft events
 */
public class ReplayableFlightDataSource {

    static final String SOURCE_MAP = "aircraftMovements";
    private static final String JOB_NAME = "flightTelemetry";

    public static JetConfig buildJetConfig() {
        JetConfig config = new JetConfig();
        config.getHazelcastConfig()
                .getMapEventJournalConfig(ReplayableFlightDataSource.SOURCE_MAP)
                .setCapacity(100_000)
                .setEnabled(true);

        MaxSizeConfig maxSizeConfig = config.getHazelcastConfig().getMapConfig(SOURCE_MAP).getMaxSizeConfig();
        maxSizeConfig.setSize(10000).setMaxSizePolicy(MaxSizeConfig.MaxSizePolicy.PER_NODE);

        return config;
    }

    public static JobConfig buildJobConfig() {
        JobConfig config = new JobConfig();
        config.setName(JOB_NAME).setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);

        return config;
    }

    public static StreamSource<Aircraft> buildSource() {
        return mapJournal(SOURCE_MAP, (DistributedPredicate<EventJournalMapEvent<Long, Aircraft>>) e -> true,
                (DistributedFunction<EventJournalMapEvent<Long, Aircraft>, Aircraft>) EventJournalMapEvent::getNewValue,
                START_FROM_OLDEST);
    }

}
