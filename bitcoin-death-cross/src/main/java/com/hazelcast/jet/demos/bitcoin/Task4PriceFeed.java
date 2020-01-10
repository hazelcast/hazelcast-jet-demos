package com.hazelcast.jet.demos.bitcoin;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.demos.bitcoin.domain.Price;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * <p>This is {@code Task4PriceFeed}. The use of the "{@code @Order}"
 * annotation tells Spring the order to run these tasks.
 * </p>
 * <p>{@code Task4PriceFeed} inserts the Bitcoin prices into a map.
 * Each new price overwrites the previous one, as all we
 * are concerned with is storing the current price. Jet
 * has access to the
 * <a href="https://docs.hazelcast.org/docs/3.12/javadoc/com/hazelcast/map/impl/journal/MapEventJournal.html">MapEventJournal</a>
 * so has the history of change to the price.
 * </p>
 * <p><b>Note:</b> {@code Task1JetJob}, {@code Task2ChartPanel} and {@code Task3TopicListener}
 * are ordered to run before {@code Task4PriceFeed}. The first three can
 * be run in any order, all will appear to do nothing until the
 * fourth starts producing data.
 * </p>
 */
@Component
@Order(MyConstants.PRIORITY_FOUR)
@Slf4j
public class Task4PriceFeed implements CommandLineRunner {

	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private HazelcastInstance hazelcastInstance;
	
	/**
	 * <p>Inject a "<i>continuous stream</i>" of prices into
	 * a Hazelcast {@link IMap}.
	 * </p>
	 * <p>In this demo, it's not really a continuous stream
	 * of prices. It's actually a captured list of prices
	 * in a file, just for simulation purposes and so the
	 * same data can easily be replayed again and again.
	 * If you want and have the credentials, you can
	 * connect this to a live exchange.
	 * </p>
	 * <p>However, Hazelcast can't tell the difference.
	 * Prices arrive periodically into the IMDG from
	 * some outside source, with no indication that
	 * this will stop.
	 * </p>
	 * <p>The prices themselves are one per day, but
	 * they could just as easily be one per millisecond.
	 * </p>
	 */
	@Override
	public void run(String... args) throws Exception {
		String prefix = this.getClass().getSimpleName() + " -";


		IMap<String, Price> pricesInMap =
				this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_PRICES_IN);
		
		String key = MyConstants.BTCUSD;
		
		if (pricesInMap.containsKey(key)) {
            log.info("{} Data '{}' exists in map '{}'.",
            		prefix, key, pricesInMap.getName());
		} else {
			log.info("{} Turn on price feed", prefix);

			List<Price> prices = this.loadPrices(key);

			for (int i = 0 ; i < prices.size() ; i++) {
				pricesInMap.set(key, prices.get(i));
				
				// Delay the insert to simulate continuous update stream
				try {
					TimeUnit.MILLISECONDS.sleep(MyConstants.PRICE_DELAY_MS);
				} catch (InterruptedException ignored) {
					;
				}
			}

            log.debug("Wrote {} prices for key '{}' into map '{}'.",
            		prices.size(), key, pricesInMap.getName());
		}
		
	}
	
	
	/**
	 * <p>Process a file of dates and prices into a
	 * list of {@link Price} objects.
	 * </p>
	 * <p>Use an {@link TreeSet} as an intermediate
	 * container to ensure we get them in collating
	 * sequence, rather than as they might occur in
	 * the file.
	 * </p>
	 * 
	 * @param currencyPair For this, "{@code BTCUSD}"
	 * @return A list of prices
	 */
	private List<Price> loadPrices(String currencyPair) {
		TreeSet<Price> prices = new TreeSet<>();
		
		String input = "classpath:" + currencyPair + ".csv";

		try {
			Resource resource = this.applicationContext.getResource(input);

			try (InputStream inputStream = resource.getInputStream();
					InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
					BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					if (line.length()>0 && !line.startsWith("#")) {
						try {
							prices.add(Task4PriceFeed.parse(line));
						} catch (Exception e) {
							String message =
									String.format("Problem with '%s%' : %s",
											line, e.getMessage());
							log.error(message);
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Problem reading '" + input + "'", e);
		}

		return new ArrayList<>(prices);
	}

	/**
	 * <p>Simplistic parsing for a line from a CSV file,
	 * no error handling just throws an exception.
	 * </p>
	 * <p>Expected input format "{@code CCYY-MM-DD,nnn.mm}"
	 * for a date and an exchange rate.
	 * </p>
	 * 
	 * @param line From a csv file
	 * @return A {@link Price}
	 * @throws Exception
	 */
	private static Price parse(String line) throws Exception {
		String[] tokens = line.split(",");

		LocalDate localDate =
				LocalDate.parse(tokens[0], DateTimeFormatter.ISO_LOCAL_DATE);
		
		BigDecimal rate = new BigDecimal(tokens[1]);

		Price price = new Price();
		price.setLocalDate(localDate);
		price.setRate(rate);

		return price;
	}
	
}
