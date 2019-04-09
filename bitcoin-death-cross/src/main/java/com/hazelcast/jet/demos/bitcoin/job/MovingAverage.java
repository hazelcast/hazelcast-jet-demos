package com.hazelcast.jet.demos.bitcoin.job;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation2;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.demos.bitcoin.MyConstants;
import com.hazelcast.jet.demos.bitcoin.domain.Price;
import com.hazelcast.jet.function.FunctionEx;
import com.hazelcast.jet.function.Functions;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StageWithKeyAndWindow;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.pipeline.StreamStageWithKey;
import com.hazelcast.jet.pipeline.WindowDefinition;

/**
 * TODO Javadoc
 * TODO Confirm names on pipeline stages
 */
public class MovingAverage {

	/**
	 * <p>Break the input stream into daily chunks. As
	 * we have daily prices, each will contain one item.
	 * </p>
	 */
	public static final WindowDefinition ONE_DAY_WINDOW =
    		WindowDefinition.tumbling(TimeUnit.DAYS.toMillis(1));

	/**
	 * <p>Our input data is sorted (by us) so we don't need
	 * to allow for late, out-of-sequence items.
	 * </p>
	 */
    public static final long ZERO_LAG = 0;

    /**
     * <p>Convenience function to make a string key from
     * a price based on the date.
     * </p>
     * 
     * @return Date as string
     */
	private static FunctionEx<Entry<String, Price>, String> whence() {
        return e -> e.getValue().getLocalDate().toString();
    }

    /**
     * <p>Build the pipeline definition, to send to
     * all JVMs for execution.
     * </p>
     */
	public static Pipeline build() {
		Pipeline pipeline = Pipeline.create();

		//TODO Javadoc
		StreamStageWithKey<Entry<String, Price>, String> priceFeed = 
				pipeline.drawFrom(
						Sources.<String,Price>mapJournal(
									MyConstants.IMAP_NAME_PRICES_IN,
									JournalInitialPosition.START_FROM_OLDEST)
							)
				.withoutTimestamps()
				.setName("priceFeed")
				.groupingKey(Functions.entryKey())
				;
		

		//TODO Javadoc
		StreamStage<Entry<String,Price>> averageOf1 =
				priceFeed.customTransform("averageOf1-noop", 
						() -> new SimpleMovingAverage(1)
				);
		StreamStage<Entry<String,Price>> averageOf50 =
				priceFeed.customTransform("averageOf50", 
						() -> new SimpleMovingAverage(50)
				);
		StreamStage<Entry<String,Price>> averageOf200 =
				priceFeed.customTransform("averageOf200", 
						() -> new SimpleMovingAverage(200)
				);


		//TODO Javadoc
		averageOf1
			.drainTo(Sinks.logger())
			.setName("logSink");

		averageOf1
			.drainTo(Sinks.map(MyConstants.IMAP_NAME_PRICES_OUT_BTCUSD))
			.setName("mapSink-" + MyConstants.KEY_CURRENT);
		averageOf50
			.drainTo(Sinks.map(MyConstants.IMAP_NAME_PRICES_OUT_BTCUSD))
			.setName("mapSink-" + MyConstants.KEY_50_POINT);
		averageOf200
			.drainTo(Sinks.map(MyConstants.IMAP_NAME_PRICES_OUT_BTCUSD))
			.setName("mapSink-" + MyConstants.KEY_200_POINT);

		//TODO Javadoc - key is "50-point"
		StageWithKeyAndWindow<Entry<String, Price>, String> windowOf50 =
				averageOf50
				.addTimestamps(e -> e.getValue().getTimestamp(), ZERO_LAG)
				.window(ONE_DAY_WINDOW)
				.groupingKey(MovingAverage.whence())
				;

		//TODO Javadoc - key is "200-point"
		StreamStageWithKey<Entry<String, Price>, String> windowOf200 =
				averageOf200
				.addTimestamps(e -> e.getValue().getTimestamp(), ZERO_LAG)
				.groupingKey(MovingAverage.whence())
				;

		//TODO Javadoc - left, right, accumulator, result
		AggregateOperation2
			<Entry<String, Price>, Entry<String, Price>, 
				MyPriceAccumulator, Tuple3<LocalDate, BigDecimal, BigDecimal>> 
			myAggregateOperation
			= AggregateOperation
				.withCreate(MyPriceAccumulator::new)
			     .<Entry<String, Price>>andAccumulate0(
			    		 (MyPriceAccumulator myPriceAccumulator, Entry<String, Price> entry)
			    		 -> myPriceAccumulator.setLeft(entry.getValue())
			    		 )
			     .<Entry<String, Price>>andAccumulate1(
			    		 (MyPriceAccumulator myPriceAccumulator, Entry<String, Price> entry)
			    		 -> myPriceAccumulator.setRight(entry.getValue())
			    		 )
			     .andExportFinish(MyPriceAccumulator::result);

		//TODO Javadoc
		StreamStageWithKey<SimpleImmutableEntry<String, Tuple3<LocalDate, BigDecimal, BigDecimal>>, String> 
			joined50point200point
			= windowOf50
				.aggregate2(windowOf200,myAggregateOperation)
				.map(
					(KeyedWindowResult<String, Tuple3<LocalDate, BigDecimal, BigDecimal>> entry) 
					->
					new SimpleImmutableEntry<String, Tuple3<LocalDate, BigDecimal, BigDecimal>>
						(MyConstants.BTCUSD, entry.getValue()))
				.groupingKey(Functions.entryKey());

		//TODO Javadoc - optional save to IMap as Entry
		StreamStage<Entry<?,?>> alerts =
				joined50point200point
				.customTransform("crossEmitter", CrossEmitter::new);

		//TODO Javadoc
		Sink<? super Entry<?, ?>> alertSink = 
				SinkBuilder.sinkBuilder(
						"topicSink-" + MyConstants.ITOPIC_NAME_ALERT, 
						context -> context.jetInstance().getHazelcastInstance().getTopic(MyConstants.ITOPIC_NAME_ALERT)
						)
				.receiveFn((iTopic, item) -> iTopic.publish(item))
				.build();
		alerts
			.drainTo(alertSink);
		
		return pipeline;
	}    

}
