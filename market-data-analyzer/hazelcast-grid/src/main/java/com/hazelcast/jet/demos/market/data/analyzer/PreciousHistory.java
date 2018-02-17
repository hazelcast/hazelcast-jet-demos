package com.hazelcast.jet.demos.market.data.analyzer;

import static com.hazelcast.jet.core.WatermarkGenerationParams.noWatermarks;

import com.hazelcast.jet.JournalInitialPosition;
import com.hazelcast.jet.Pipeline;
import com.hazelcast.jet.Sinks;
import com.hazelcast.jet.Sources;

public class PreciousHistory {

    public static Pipeline build() {

        Pipeline pipeline = Pipeline.create();
        
        // Palladium and Platinum only
        pipeline
        .drawFrom(Sources.<String, Object>mapJournal(Constants.IMAP_NAME_PRECIOUS, JournalInitialPosition.START_FROM_OLDEST, noWatermarks()))
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
