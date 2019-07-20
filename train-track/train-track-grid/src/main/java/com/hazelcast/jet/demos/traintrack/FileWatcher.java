package com.hazelcast.jet.demos.traintrack;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamSource;

import lombok.extern.slf4j.Slf4j;

/**
 * XXX
 */
@Slf4j
public class FileWatcher {

	static Pipeline build() {
		Pipeline pipeline = Pipeline.create();
		
		pipeline
		.drawFrom(FileWatcher.buildFileWatcherSource()).withoutTimestamps().setName("fileSource")
		.filter(s -> {
			log.debug(s);
			return true;
		})
		//TODO Derive speed
        .drainTo(FileWatcher.buildTopicSink());
		
		return pipeline;
	}

	
	/**
	 * XXX
	 *
	 * @return
	 */
	protected static StreamSource<String> buildFileWatcherSource() {
		return Sources.filesBuilder(".").glob("beam-output-*").buildWatcher();
	}
	
	/**
	 * XXX
	 *
	 * @return
	 */
    protected static Sink<? super String> buildTopicSink() {
        return SinkBuilder.sinkBuilder(
                        "topicSink", 
                        context -> context.jetInstance().getHazelcastInstance().getTopic("treno")
                        )
                        .receiveFn((iTopic, item) -> iTopic.publish(new HazelcastJsonValue(item.toString())))
                        .build();
    }
	
}
