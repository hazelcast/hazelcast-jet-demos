package com.hazelcast.jet.demos.bitcoin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.demos.bitcoin.job.MovingAverage;
import com.hazelcast.jet.pipeline.Pipeline;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>This is {@code Task1}. The use of the "{@code @Order}"
 * annotation tells Spring the order to run these tasks.
 * </p>
 * <p>{@code Task1} initiates the Jet job to process Moving
 * Averages. The Jet job sits waiting for input, which is
 * produced by a later task ({@code Task4}) simulating a 
 * real-world feed.
 * </p>
 * <p><b>Note:</b> {@code Task1}, {@code Task2} and {@code Task3}
 * are ordered to run before {@code Task4}. The first three can
 * be run in any order, all will appear to do nothing until the
 * fourth starts producing data.
 * </p>
 */
@Component
@Order(MyConstants.PRIORITY_ONE)
@Slf4j
public class Task1 implements CommandLineRunner {

	@Autowired
	private JetInstance jetInstance;
	
	/**
	 * <p>Run one copy of the Moving Average job
	 * in this cluster.
	 * </p>
	 */
	@Override
	public void run(String... args) throws Exception {
		log.info("{} - Start Jet job", this.getClass().getSimpleName());

    	Pipeline pipeline = MovingAverage.build();
    	JobConfig jobConfig = new JobConfig();
    	jobConfig.setName(MyConstants.JOB_NAME);

    	// Run job if not already present
		Job job = this.jetInstance.getJob(jobConfig.getName());
    	if (job!=null) {
            log.info("Job '{}' exists, status '{}'.",
            		job.getName(), job.getStatus());
    	} else {
        	job = this.jetInstance.newJob(pipeline, jobConfig);
            log.info("Started job '{}'.", job.getName());
    	}
	}

}
