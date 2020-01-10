package com.hazelcast.jet.demos.bitcoin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.jet.demos.bitcoin.alerting.MyTopicListener;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>This is {@code Task3TopicListener}. The use of the "{@code @Order}"
 * annotation tells Spring the order to run these tasks.
 * </p>
 * <p>{@code Task3TopicListener} adds a broadcast listener on a 
 * {@link com.hazelcast.core.ITopic ITopic}, so all members
 * in the cluster are alerted if the price of Bitcoin
 * does something interesting.
 * </p>
 * <p><b>Note:</b> {@code Task1JetJob}, {@code Task2ChartPanel} and {@code Task3TopicListener}
 * are ordered to run before {@code Task4PriceFeed}. The first three can
 * be run in any order, all will appear to do nothing until the
 * fourth starts producing data.
 * </p>
 */
@Component
@Order(MyConstants.PRIORITY_THREE)
@Slf4j
public class Task3TopicListener implements CommandLineRunner {

	@Autowired
	private HazelcastInstance hazelcastInstance;
	@Autowired
	private MyTopicListener myTopicListener;
	
	@Override
	public void run(String... args) throws Exception {
		String prefix = this.getClass().getSimpleName() + " -";

		log.info("{} Start topic listener", prefix);

		ITopic<Object> iTopic =
				this.hazelcastInstance
				.getTopic(MyConstants.ITOPIC_NAME_ALERT);
		
		iTopic.addMessageListener(myTopicListener);
	}
}
