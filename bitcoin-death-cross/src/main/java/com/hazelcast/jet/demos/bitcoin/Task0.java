package com.hazelcast.jet.demos.bitcoin;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>This is {@code Task0}. The use of the "{@code @Order}"
 * annotation tells Spring the order to run these tasks.
 * </p>
 * <p>Wait a few seconds (10 seconds) after this JVM starts
 * to allow others to start to and join in the cluster.
 * </p>
 * <p>This means when {@code Task1} initiates the Jet job
 * the cluster isn't going to change size while the job
 * runs, and the logs don't have the extra complication
 * of job rebalancing and restarted execution.
 * </p>
 */
@Component
@Order(MyConstants.PRIORITY_ZERO)
@Slf4j
public class Task0 implements CommandLineRunner {

	@Autowired
	private HazelcastInstance hazelcastInstance;
	
	/**
	 * <p>Sleep briefly to allow cluster to form.
	 * Note we don't skip the sleep if we are the 2nd to join,
	 * as there could be a 3rd or a 4th.
	 * </p>
	 */
	@Override
	public void run(String... args) throws Exception {
		String prefix = this.getClass().getSimpleName() + " -";

		// Force create partitions
		this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_PRICES_IN);

        int clusterSize = this.hazelcastInstance.getCluster().getMembers().size();

        // Wait a bit for others
        log.info("{} Cluster size {}, wait {} sec",
            		prefix, clusterSize, MyConstants.STARTUP_DELAY_SECONDS);
        TimeUnit.SECONDS.sleep(MyConstants.STARTUP_DELAY_SECONDS);
	}

}
