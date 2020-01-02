package com.hazelcast.jet.demos.bitcoin.job;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleImmutableEntry;

import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;

/**
 * <p>Compare one pair of prices with the immediately preceding
 * pair of prices.
 * </p>
 * <p>The "{@code left}" field is the 50-point moving average and
 * the "{@code right}" is the 200-point.
 * </p>
 * <p>If they cross, one going up while the other is down, produce
 * some output. Otherwise keep silent.
 * </p>
 * <p><b>Note: </b> As the 50-point moving average starts 150 days
 * before the 200-point moving average the first 150 pairs of
 * prices are incomplete, missing the 200-point "{@code right}"
 * value. We discard those too, but we could just as easily
 * filter them out in the pipeline itself before passing to this
 * routine, and this would be fractionally more efficient.
 * </p>
 * <p>Since the days start from 2017-01-01, the first 50-point is
 * on 2017-02-17, and the first 200-point on 2017-07-19.
 * </p>
 */
public class CrossDetector {
			
	public SimpleImmutableEntry<Tuple2<LocalDate, String>, Tuple2<BigDecimal, BigDecimal>>
		consider(
				Tuple2< 
					Tuple3<LocalDate, BigDecimal, BigDecimal>,
					Tuple3<LocalDate, BigDecimal, BigDecimal>
			 	> pair) {
		
		Tuple3<LocalDate, BigDecimal, BigDecimal> priceBefore = pair.f0();
		Tuple3<LocalDate, BigDecimal, BigDecimal> priceAfter = pair.f1();
		
		BigDecimal yesterday50point = priceBefore.f1();
		BigDecimal yesterday200point = priceBefore.f2();
		BigDecimal today50point = priceAfter.f1();
		BigDecimal today200point = priceAfter.f2();
		
		// Business logic, do they cross ?
		String trend = null;
		if (yesterday50point.compareTo(yesterday200point) < 0) {
			if (today50point.compareTo(today200point) > 0) {
				trend = "Upward";
			}
		}
		if (yesterday50point.compareTo(yesterday200point) > 0) {
			if (today50point.compareTo(today200point) < 0) {
				trend = "Downward";
			}
		}
		
		if (trend != null) {
			Tuple2<LocalDate, String> resultKey
				= Tuple2.tuple2(priceAfter.f0(), trend);
			
			Tuple2<BigDecimal, BigDecimal> resultValue
				= Tuple2.tuple2(today50point, today200point);

			return new SimpleImmutableEntry<>(resultKey, resultValue);
		} else {
			return null;
		}
	}
 
}
