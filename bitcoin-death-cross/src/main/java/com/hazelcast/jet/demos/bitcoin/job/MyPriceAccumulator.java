package com.hazelcast.jet.demos.bitcoin.job;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.demos.bitcoin.domain.Price;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Merge two price streams, 50-point on the left
 * and 200-point on the right. As the 50-point starts
 * first it will always exist, for the first 150
 * observations the right stream (200-point) will have
 * no matching value.
 * </p>
 */
@Data
@Slf4j
public class MyPriceAccumulator {

	private LocalDate date;
	private BigDecimal left;
	private BigDecimal right;

	/**
	 * <p>Apply a new data from the 50-point stream into
	 * the current accumulator.
	 * </p>
	 *
	 * @param arg0 The 50-point stream
	 * @return A trio combining 50-point and 200-point
	 */
	public MyPriceAccumulator setLeft(Price arg0) {
		this.left = arg0.getRate();
		if (this.date != null) {
			if (!this.date.isEqual(arg0.getLocalDate())) {
				log.error("Date clash: {} but setLeft({})", this, arg0);
			}
		} else {
			this.date = arg0.getLocalDate();
		}
		return this;
	}

	/**
	 * <p>Apply a new data from the 200-point stream into
	 * the current accumulator.
	 * </p>
	 *
	 * @param arg0 The 200-point stream
	 * @return A trio combining 50-point and 200-point
	 */
	public MyPriceAccumulator setRight(Price arg0) {
		this.right = arg0.getRate();
		if (this.date != null) {
			if (!this.date.isEqual(arg0.getLocalDate())) {
				log.error("Date clash: {} but setRight({})", this, arg0);
			}
		} else {
			this.date = arg0.getLocalDate();
		}
		return this;
	}

	/**
	 * <p>Merge two accumulators together, eg. if accumulated on
	 * another CPU. We do not expect the incoming side to overwrite
	 * any data, so report error if so.
	 * </p>
	 *
	 * @param that The other accumulator
	 * @return A trio combining 50-point and 200-point
	 */
	public MyPriceAccumulator combine(MyPriceAccumulator that) {
		if (this.date == null) {
			this.date = that.getDate();
		} else {
			if (that.getDate() != null && !this.getDate().equals(that.getDate())) {
				log.error("Date clash: {} but combine with ({})", this, that);
			}
		}
		if (this.left == null) {
			this.left = that.getLeft();
		} else {
			if (that.getLeft() != null && !this.getLeft().equals(that.getLeft())) {
				log.error("Left clash: {} but combine with ({})", this, that);
			}
		}
		if (this.right == null) {
			this.right = that.getRight();
		} else {
			if (that.getLeft() != null && !this.getRight().equals(that.getRight())) {
				log.error("Right clash: {} but combine with ({})", this, that);
			}
		}
		return this;
	}

	public Tuple3<LocalDate, BigDecimal, BigDecimal> result() {
		return Tuple3.tuple3(this.date, this.left, this.right);
	}

}
