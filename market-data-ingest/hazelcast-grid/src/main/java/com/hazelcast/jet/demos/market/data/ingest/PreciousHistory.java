package com.hazelcast.jet.demos.market.data.ingest;

import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;

public class PreciousHistory {

    public static Pipeline build() {
        Pipeline p = Pipeline.create();

        // Palladium and Platinum only
        p.readFrom(Sources.<String, Object>mapJournal(
                Constants.IMAP_NAME_PRECIOUS, JournalInitialPosition.START_FROM_OLDEST)
        ).withoutTimestamps()
         .map(e -> e.getKey() + "==" + e.getValue())
         .filter(str -> str.toLowerCase().startsWith("p"))
         .writeTo(Sinks.logger());

        return p;
    }

}
