package com.hazelcast.jet.demos.bitcoin.job;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.demos.bitcoin.domain.Price;

import lombok.Data;

/**
 * <p>Merge two price streams, 50-point on the left
 * and 200-point on the right. As the 50-point starts
 * first it will always exist, for the first 150
 * observations the right stream (200-point) will have
 * no matching value.
 * </p>
 * <p>This is almost trivial enough to do with lambdas.
 * </p>
 */
@Data
public class MyPriceAccumulator {

	private LocalDate date;
	private BigDecimal left;
	private BigDecimal right;
	
	public MyPriceAccumulator setLeft(Price arg0) {
		this.date = arg0.getLocalDate();
		this.left = arg0.getRate();
		return this;
	}

	public MyPriceAccumulator setRight(Price arg0) {
		this.right = arg0.getRate();
		return this;
	}

	public Tuple3<LocalDate, BigDecimal, BigDecimal> result() {
		return Tuple3.tuple3(this.date, this.left, this.right);
	}

}
