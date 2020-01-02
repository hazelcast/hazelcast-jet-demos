package com.hazelcast.jet.demos.bitcoin.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

import lombok.Data;

/**
 * <p>A domain model for the price of something at a
 * particular point in time.
 * </p>
 * <p>We use {@link LocalDate} for the price, as we don't
 * have hours, minutes, seconds or anything more granular.
 * </p>
 * <p>We use {@link BigDecimal} instead of {@code double}
 * just to show we are aware of numeric accuracy.
 * </p>
 */
@SuppressWarnings("serial")
@Data
public class Price implements Comparable<Price>, Serializable {

	private LocalDate localDate;
	private BigDecimal rate;
	
	@Override
	public int compareTo(Price that) {
		return this.localDate.compareTo(that.getLocalDate());
	}

	/**
	 * <p>Turn the date into a standard timestamp.
	 * </p>
	 * @return A non-zero positive number, all prices are historical
	 */
        public long getTimestamp() {
            return this.localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        }

}
