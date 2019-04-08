package com.hazelcast.jet.demos.bitcoin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.demos.bitcoin.charting.PricePanelListener;
import com.hazelcast.jet.demos.bitcoin.domain.Price;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>This is {@code Task2}. The use of the "{@code @Order}"
 * annotation tells Spring the order to run these tasks.
 * </p>
 * <p>{@code Task2} initiates a graphic panel that is painted
 * in reaction to data being written by the Jet job {@code Task1}.
 * It plots the points that Jet calculates.
 * </p>
 * <p><b>Note:</b> {@code Task1}, {@code Task2} and {@code Task3}
 * are ordered to run before {@code Task4}. The first three can
 * be run in any order, all will appear to do nothing until the
 * fourth starts producing data.
 * </p>
 */
@Component
@Order(MyConstants.PRIORITY_TWO)
@Slf4j
public class Task2 implements CommandLineRunner {

    @Autowired
    private HazelcastInstance hazelcastInstance;
    
    /**
     * <p>Create a chart on screen that plots points
     * as they are produced by Jet. The input to Jet
     * is throttled so they don't come out too fast,
     * makes the plot look better.
     * </p>
     * <p>Since the Hazelcast cluster might have
     * multiple members, and we only really want the
     * chart once, use a counter to ensure the
     * chart is only produced by one member. If
     * the new (3.12) CP sub-system is available,
     * use that in preference to the deprecated
     * "{@code hazelcastInstance.getAtomicLong(String s)}"
     * </p>
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("{} - Start graph plot", this.getClass().getSimpleName());

        IMap<String, Price> pricesOutMap =
            this.hazelcastInstance
                .getMap(MyConstants.IMAP_NAME_PRICES_OUT_BTCUSD);

        pricesOutMap
            .addEntryListener(new PricePanelListener(), true);
    }
}
