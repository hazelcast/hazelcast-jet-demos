package com.hazelcast.jet.demos.traintrack;

import org.apache.beam.sdk.transforms.DoFn;

/**
 * <p>The input element of the process context is a CSV string.
 * Convert this into JSON.
 * </p>
 * <p>The third part of the CSV is a time offset, in milliseconds.
 * Enrich this with the current time to make the timestamps in the
 * output JSON appear to be happening right now.
 * </p>
 */
@SuppressWarnings("serial")
public class MyEnrichAndReformatFn extends DoFn<String, String> {

	private final long startTimestamp;

	MyEnrichAndReformatFn(long arg0) {
		this.startTimestamp = arg0;
	}

	/**
	 * <p>Use a helper function to take the input from the
	 * execution context to make the output.
	 * </p>
	 *
	 * @param processContext
	 */
    @ProcessElement
    public void processElement(ProcessContext processContext) { 
    	processContext.output(this._process(processContext.element()));
	}

    /**
     * <p>Separate out the reformatting from execution context access
     * for easier unit testing.
     * </p>
     *
     * @param input String in CSV form
     * @return String in JSON form
     */
    public String _process(String input) {
    	String[] trio = input.split(",");
    	
        StringBuilder sb = new StringBuilder("{");

        sb.append(" \"latitude\": \"" + trio[0] + "\",");
        sb.append(" \"longitude\": \"" + trio[1] + "\",");

        // Field 3 is offset in seconds.
        long timestamp = this.startTimestamp + 1000 * Long.parseLong(trio[2]);
        sb.append(" \"timestamp\": \"" + timestamp + "\"");

        sb.append(" }");
        
        return sb.toString();
    }
}
