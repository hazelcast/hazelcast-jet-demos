package com.hazelcast.jet.demos.market.data.analyzer;

import com.hazelcast.jet.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;

public class PreciousHistory {

    public static Pipeline build() {

        Pipeline pipeline = Pipeline.create();

        // Palladium and Platinum only
        pipeline
                .drawFrom(Sources.<String, Object>mapJournal(Constants.IMAP_NAME_PRECIOUS, JournalInitialPosition.START_FROM_OLDEST))
                .map(entry
                        ->
                        new String(entry.getKey() + "==" + entry.getValue())
                )
                .filter(str -> str.toLowerCase().startsWith("p"))
                .drainTo(Sinks.logger())
        ;

        return pipeline;
    }

}
