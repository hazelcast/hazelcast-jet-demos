package com.hazelcast.jet.demos.traintrack;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.ParDo;

/**
 * <p>Define a Beam pipeline that reads a CSV file, reformats it
 * to JSON, and writes it to another file. Reformatting uses the
 * start time of the job adjust the timestamp on the input data
 * so that it looks like it is happening real-time.
 * </p>
 * <p>This is not a particularly sophisticated task, but this
 * example isn't about the business logic.
 * </p>
 */
public class MyBeamJob {

	public static Pipeline build(PipelineOptions pipelineOptions) {
		
		long start = System.currentTimeMillis();
		
	    Pipeline pipeline = Pipeline.create(pipelineOptions);

	    pipeline
	    .apply("source", TextIO.read().from("beam-input"))
	    .apply("reformat", ParDo.of(new MyEnrichAndReformatFn(start)))
        .apply("sink", TextIO.write().to("beam-output"));

	    return pipeline;
	}

}
