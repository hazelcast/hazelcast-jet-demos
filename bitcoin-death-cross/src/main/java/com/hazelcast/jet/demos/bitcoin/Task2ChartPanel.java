package com.hazelcast.jet.demos.bitcoin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.demos.bitcoin.alerting.MyTopicListener;
import com.hazelcast.jet.demos.bitcoin.charting.PricePanelListener;
import com.hazelcast.jet.demos.bitcoin.domain.Price;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>This is {@code Task2ChartPanel}. The use of the "{@code @Order}"
 * annotation tells Spring the order to run these tasks.
 * </p>
 * <p>{@code Task2ChartPanel} initiates a graphic panel that is painted
 * in reaction to data being written by the Jet job {@code Task1JetJob}.
 * It plots the points that Jet calculates.
 * </p>
 * <p><b>Note:</b> {@code Task1JetJob}, {@code Task2ChartPanel} and {@code Task3TopicListener}
 * are ordered to run before {@code Task4PriceFeed}. The first three can
 * be run in any order, all will appear to do nothing until the
 * fourth starts producing data.
 * </p>
 */
@Component
@Order(MyConstants.PRIORITY_TWO)
@Slf4j
public class Task2ChartPanel implements CommandLineRunner {

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private MyTopicListener myTopicListener;
    
    /**
     * <p>Create a chart on screen that plots points
     * as they are produced by Jet. The input to Jet
     * is throttled so they don't come out too fast,
     * makes the plot look better.
     * </p>
     */
    @Override
    public void run(String... args) throws Exception {
		String prefix = this.getClass().getSimpleName() + " -";

    	log.info("{} Adding chart.", prefix);
 
    	IMap<String, Price> pricesOutMap =
    		this.hazelcastInstance
    			.getMap(MyConstants.IMAP_NAME_PRICES_OUT_BTCUSD);

        pricesOutMap
        	.addEntryListener(new PricePanelListener(this.myTopicListener), true);
    }
}
