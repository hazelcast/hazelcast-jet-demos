package com.hazelcast.jet.demos.bitcoin.job;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.demos.bitcoin.MyConstants;
import com.hazelcast.jet.demos.bitcoin.domain.Price;

/**
 * <p>Calculate the simple average of a some points
 * arriving.
 * </p>
 * <p>"<i>Simple</i>" here refers to the usual way
 * of adding up all the points and dividing by the
 * count. So the input "{@code 1,2,3,4,5}" should
 * give the answer "{@code 3}".
 * </p>
 * <p>Other ways exist. For example repeating the
 * later numbers to reflect their recentness makes
 * their values more important. So input 
 * "{@code 1,2,3,4,5}" might be calculated as
 * the sum of "{@code 1,2,3,4,4,5,5}" divided by 
 * 7 giving "{@code 3.43}".
 * </p>
 * <p><b>NOTE:</b> This implementation is based on
 * the assumption that the input stream is strictly
 * consecutive. Item 1 arrives, then item 2, then
 * item 3, etc. There can be no gaps or out of sequence
 * items, or the wrong output will occur. If that is
 * possible, it needs better logic. This is just an
 * example after all, we can make this assumption
 * as the data used is pre-set and sorted.
 * </p>
 */
public class SimpleMovingAverage
	extends AbstractProcessor {

	private final BigDecimal[] rates;
	private final BigDecimal size;
	private final String key;
	private int current;
	
	public SimpleMovingAverage(int i) {
		this.rates = new BigDecimal[i];
		this.size = new BigDecimal(i);
		
		switch(i) {
		case 50: this.key = MyConstants.KEY_50_POINT;
			break;
		case 200: this.key = MyConstants.KEY_200_POINT;
			break;
		default /* 1 */: this.key = MyConstants.KEY_CURRENT;
			break;
		}
		
	}

	/**
	 * <p>Accumulate input in a ringbuffer, implemented
	 * using an array.
	 * </p>
	 * <p>As the array is filling, no output is produced
	 * but we return "{@code true}" so Jet knows all is
	 * good with input processing.
	 * </p>
	 * <p>Once the array is full, for each number of input
	 * we place in the appropriate part of the array,
	 * calculate the average and try to output. Potentially
	 * the output queue is full. It's not guaranteed that
	 * there is room for our output to be accepted, so cater
     * for this.
	 * </p>
	 */
	@Override
	protected boolean tryProcess(int ordinal, Object item) {
		
		@SuppressWarnings("unchecked")
		Price price = ((Entry<String, Price>) item).getValue();
		
		// Store the value
		this.rates[this.current] = price.getRate();

		// Determine the next slot
		int next;
		if (this.current == (this.rates.length - 1)) {
			next = 0;
		} else {
			next = this.current + 1;
		}
		
		// Try to output an average, if we have enough stored input
		if (this.rates[next]==null) {
			this.current = next;
			return true;
		} else {
			Price average = new Price();
			average.setLocalDate(price.getLocalDate());
			average.setRate(this.calculateAverage());
			
			Entry<String,Price> result
				= new SimpleImmutableEntry<>(this.key, average);
			
			// If we can output, advance the next write location
			boolean emit = super.tryEmit(result);
			if (emit) {
				this.current = next;
			}
			return emit;
		}
	}
	
	/**
	 * <p>Add all the points up and divide by how many
	 * points there are.
	 * </p>
	 * 
	 * @return
	 */
	private BigDecimal calculateAverage() {
		BigDecimal sum = BigDecimal.ZERO;
		
		for (int i = 0 ; i < this.rates.length ; i ++) {
			sum = sum.add(this.rates[i]);
		}
		
		// Two decimal places
		BigDecimal average = sum.divide(size, 2, RoundingMode.HALF_UP);
		
		return average;
	}

}
