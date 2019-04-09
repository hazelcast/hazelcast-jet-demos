package com.hazelcast.jet.demos.bitcoin.job;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.impl.JetEvent;

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
 */
public class CrossEmitter
	extends AbstractProcessor {
	
	private Tuple3<LocalDate, BigDecimal, BigDecimal> previous = null;
	
	@Override
	protected boolean tryProcess(int ordinal, Object item) {
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Entry<String, Tuple3<LocalDate, BigDecimal, BigDecimal>> entry = 
			(Entry<String, Tuple3<LocalDate, BigDecimal, BigDecimal>>) ((JetEvent) item).payload();
		
		Tuple3<LocalDate, BigDecimal, BigDecimal> current = entry.getValue();

		// Ignore incomplete pairs, 50-point (f1) begins before 200-point (f2)
		if (current.f2()==null) {
			return true;
		}
		
		// Start local state
		if (this.previous == null) {
			this.previous = current;
			return true;
		}

		BigDecimal yesterday50point = this.previous.f1();
		BigDecimal yesterday200point = this.previous.f2();
		BigDecimal today50point = current.f1();
		BigDecimal today200point = current.f2();
		
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
		
		// If they do, output
		if (trend != null) {
			Tuple2<LocalDate, String> resultKey
				= Tuple2.tuple2(current.f0(), trend);
			
			Tuple2<BigDecimal, BigDecimal> resultValue
				= Tuple2.tuple2(today50point, today200point);

			Entry<Tuple2<LocalDate, String>, Tuple2<BigDecimal, BigDecimal>> resultEntry
				= new SimpleImmutableEntry<>(resultKey, resultValue);

			JetEvent<Entry<Tuple2<LocalDate, String>, Tuple2<BigDecimal, BigDecimal>>> result = 
					JetEvent.jetEvent(((JetEvent<?>) item).timestamp(), resultEntry);
		
			boolean emit = super.tryEmit(result);
			if (emit) {
				this.previous = current;
			}
			return emit;
		}

		// Nothing to emit
		this.previous = current;
		return true;
	}

}
