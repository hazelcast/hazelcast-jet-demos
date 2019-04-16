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
 * <p>Creates a processing pipeline to calculate moving
 * averages and detect when they cross.
 * </p>
 * <p>This is sequential, the output from one step only
 * goes to subsequent steps. There are no loops, as this
 * can bring deadlock.
 * </p>
 * <p>However, unlike a Unix style pipeline, it is not
 * linear. The output from a step can be the input to
 * more than one following step.
 * </p>
 * <p>The processing looks like this:
 * </p>
 * <pre>
 * XXX
 * </pre>
 * <p>What each step does is as follows:
 * </p>
 * <ol>
 * <li><p><b>??</b> : XXX
 * </p>
 * </li>
 * <li><p><b>??</b> : XXX
 * </p>
 * </li>
 * <li><p><b>??</b> : XXX
 * </p>
 * </li>
 * <li><p><b>??</b> : XXX
 * </p>
 * </li>
 * <li><p><b>??</b> : XXX
 * </p>
 * </li>
 * <li><p><b>??</b> : XXX
 * </p>
 * </li>
 * <li><p><b>??</b> : XXX
 * </p>
 * </li>
 * <li><p><b>??</b> : XXX
 * </p>
 * </li>
 * </ol>
 * XXX Check multi-node operation
 */
public class MovingAverage {

	/**
	 * <p>Break the input stream into daily chunks. As
	 * we have daily prices, each will contain one item.
	 * </p>
	 */
	private static final WindowDefinition ONE_DAY_WINDOW =
    		WindowDefinition.tumbling(TimeUnit.DAYS.toMillis(1));

	
	/**
	 * <p>Our input data is sorted (by us) so we don't need
	 * to allow for late, out-of-sequence items.
	 * </p>
	 */
	private static final long ZERO_LAG = 0;

    
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
     * <p>Build the pipeline definition, to send to all JVMs for execution.
     * </p>
     */
	public static Pipeline build() {
		Pipeline pipeline = Pipeline.create();

		/** <p> Create a single feed of prices from changes
		 * to an {@link com.hazelcast.core.IMap IMap}.</p> 
		 */
		StreamStageWithKey<Entry<String, Price>, String> priceFeed = 
				MovingAverage.buildPriceFeed(pipeline);

		/** <p>Create three different moving averages from
		 * these prices.</p>
		 */
		StreamStage<Entry<String,Price>> averageOf1 =
				MovingAverage.buildAverageOfCount(1, priceFeed);
		StreamStage<Entry<String,Price>> averageOf50 =
				MovingAverage.buildAverageOfCount(50, priceFeed);
		StreamStage<Entry<String,Price>> averageOf200 =
				MovingAverage.buildAverageOfCount(200, priceFeed);

		/** <p><i>Optional: </i>Log the current price to the
		 * screen, to help understanding.</p>
		 */
		averageOf1
			.drainTo(Sinks.logger())
			.setName("logSink");
		
		/** <p><i>Optional: </i>Save the latest for each average
		 * to an {@link com.hazelcast.core.IMap IMap}.</p>
		 */
		averageOf1
			.drainTo(Sinks.map(MyConstants.IMAP_NAME_PRICES_OUT_BTCUSD))
			.setName("mapSink-" + MyConstants.KEY_CURRENT);
		averageOf50
			.drainTo(Sinks.map(MyConstants.IMAP_NAME_PRICES_OUT_BTCUSD))
			.setName("mapSink-" + MyConstants.KEY_50_POINT);
		averageOf200
			.drainTo(Sinks.map(MyConstants.IMAP_NAME_PRICES_OUT_BTCUSD))
			.setName("mapSink-" + MyConstants.KEY_200_POINT);

		/** <p>Create a "cursor" for joining the 50-point price stream.
		 * Advance it 1 day, and therefore 1 point, at a time.
		 * </p>
		 */
		StageWithKeyAndWindow<Entry<String, Price>, String> windowOf50 =
				MovingAverage.buildWindow(averageOf50, 50)
				.window(ONE_DAY_WINDOW);
		
		/** <p>Create a "cursor" for joining the 200-point price stream.
		 * </p>
		 */
		StreamStageWithKey<Entry<String, Price>, String> windowOf200 =
				MovingAverage.buildWindow(averageOf200, 200);
		
		/** <p>Join the cursors on the 50-point and 200-point streams,
		 * producing output when both sides have a value for the same
		 * date.
		 * </p>
		 */
		StreamStageWithKey<SimpleImmutableEntry<String, Tuple3<LocalDate, BigDecimal, BigDecimal>>, String> 
			joined50point200point
			= MovingAverage.join(windowOf50,windowOf200);
			
		/** <p>The {@link CrossEmitter} takes matching pairs of
		 * 50-point and 200-point averages, and produces output
		 * only if this has reversed from the previous day.
		 * </p>
		 */
		StreamStage<Entry<?,?>> alerts =
			joined50point200point.customTransform("crossEmitter", CrossEmitter::new);

		/** <p>If there is anything produced by the {@link CrossEmitter}
		 * dump it to a {@link com.hazelcast.core.ITopic ITopic}. What
		 * comes out is a map entry so we could easily dump it to a
		 * {@link com.hazelcast.core.IMap IMap} instead (or as well)
		 * and use a map listener.
		 */
		alerts.drainTo(MovingAverage.buildAlertSink());
		
		return pipeline;
	}


	/**
	 * <p>{@link com.hazelcast.jet.demos.bitcoin.Task4 Task4} writes
	 * the current price of Bitcoin into an
	 * {@link com.hazelcast.core.IMap IMap}. This
	 * {@link com.hazelcast.core.IMap IMap} is defined with a
	 * {@link com.hazelcast.map.impl.journal.MapEventJournal MapEventJournal}
	 * that allows Jet to track the history of changes. Use this as a
	 * source to stream in.
	 * <p>
	 * <p>Don't bother yet with timestamps, they are added in later
	 * in the pipeline.
	 * </p>
	 * <p>Group (route) all events based on the key, which will be
	 * "{@code BTCUSD}". However many Jet nodes are running, only
	 * one will handle "{@code BTCUSD}". 
	 * </p>
	 *
	 * @param pipeline Will be empty
	 * @return The first stage of the pipeline
	 */
	protected static StreamStageWithKey<Entry<String, Price>, String> 
		buildPriceFeed(Pipeline pipeline) {
		return pipeline.drawFrom(
				Sources.<String,Price>mapJournal(
					MyConstants.IMAP_NAME_PRICES_IN,
					JournalInitialPosition.START_FROM_OLDEST)
				)
				.withoutTimestamps()
				.setName("priceFeed")
				.groupingKey(Functions.entryKey());
	}


	/**
	 * <p>Create a moving average of the last <i>n</i> prices,
	 * where the embedded timestamp is the date of the last price.
	 * </p>
	 * <p>The averaging here is the mathematical usual. If there
	 * are 50 prices, sum up 50 from the input and divide by
	 * 50. There is no weighting given to more recent prices,
	 * all are treated the same.
	 * </p>
	 * <p>As a special case, we may take the average of <i>n==1</i>.
	 * This is a "{@code no-op}", the input and the output are
	 * essentially the same. But we use it here to do re-formatting.
	 * </p>
	 *
	 * @param count How many consecutive prices to average
	 * @param priceFeed From the previous pipeline stage 
	 * @return The moving average
	 */
	protected static StreamStage<Entry<String,Price>>
		buildAverageOfCount(int count, StreamStageWithKey<Entry<String, Price>, String> priceFeed) {
		
			String stageName = ( count == 1 ? 
								"averageOf1-noop" :
								"averageOf" + count );
			
			return priceFeed.customTransform(stageName, 
					() -> new SimpleMovingAverage(count)
			);
	}

	
	/**
	 * <p>Take a stream of prices that happen to be a derived (average)
	 * stream of prices, and create a window on that stream. Based the window
	 * on the timestamp which is the last date of the average.
	 * </p>
	 *
	 * @param averageOfSomething Average of 50 points or the average of 200 points
	 * @param count For building the stage name
	 * @return A window on this input
	 */
	private static StreamStageWithKey<Entry<String, Price>, String> 
		buildWindow(StreamStage<Entry<String, Price>> averageOfSomething, int count) {
		return averageOfSomething
				.addTimestamps(e -> e.getValue().getTimestamp(), ZERO_LAG)
				.setName("windowOf" + count)
				.groupingKey(MovingAverage.whence())
				;
	}
	
	
	/**
	 * <p>Define an {@link AggregateOperation2} that can be used to join
	 * two streams.
	 * </p>
	 * <p>The method signature has four parts as follows:
	 * </p>
	 * <ol>
	 * <li><p><code>Entry&lt;String, Price&gt;</code> : The left input
	 * stream, known here as accumulator 0. This is a stream of averaged
	 * prices that happen to be 50-point. As 50-point starts before 200-point
	 * this stream won't be empty.
	 * </p>
	 * <li><p><code>Entry&lt;String, Price&gt;</code> : The right input
	 * stream, known here as accumulator 1. This is a stream of averaged
	 * prices on the 200-point method. As this starts 150 days after the
	 * 50-point, the first 150 items on this side will be missing.
	 * </p>
	 * <li><p><code>MyPriceAccumulator</code> : A class that combines
	 * the left and right inputs, {@link MyPriceAccumulator}. This
	 * does nothing more than capture the left and right values from
	 * the input.
	 * </p>
	 * <li><p><code>Tuple3&lt;LocalDate, BigDecimal, BigDecimal&gt;</code> :
	 * the output from the aggregation, a trio ({@link Tuple3}) of the date
	 * of the matching pair, the value from the left input (50-point) and
	 * from the right input (200-point).
	 * </p>
	 * </ol>
	 *
	 * @return An {@link AggregateOperation2} operation, combines two streams
	 */
	private static AggregateOperation2<
				Entry<String, Price>, 
				Entry<String, Price>,
				MyPriceAccumulator, 
				Tuple3<LocalDate, BigDecimal, BigDecimal>> 
		buildAggregateOperation() {
		return AggregateOperation
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
	}

	
	/**
	 * <p>Join the 50-point and 200-point streams, matching on the date
	 * for each. 
	 * </p>
	 * <p>For the 50-point stream, if there is a value for "{@code 2017-12-31}"
	 * this represents the average of 50 dates from "{@code 2017-11-16}" to
	 * "{@code 2017-12-31}".
	 * </p>
	 * <p>For the 200-point stream, if there is a value for "{@code 2017-12-31}"
	 * this represents the average of 200 dates from "{@code 2017-06-15}" to
	 * "{@code 2017-12-31}".
	 * </p>
	 * <p>Since the left stream (50-point) starts 150 days before the right
	 * stream (200-point), the first 150 output records will be incomplete.
	 * We could suppress them here, but instead we let the next stage do
	 * this.
	 * </p>
	 * <p>As a bonus, reformat this output into a map entry, as this is a
	 * more convenient format for us.
	 * </p>
	 * 
	 * @param windowOf50 The current price on the 50-point average stream
	 * @param windowOf200 The current price on the 200-point average stream
	 * @return A trio of current from 50-point and 200-point and the date
	 */
	private static StreamStageWithKey<SimpleImmutableEntry<String, Tuple3<LocalDate, BigDecimal, BigDecimal>>, String> join(
			StageWithKeyAndWindow<Entry<String, Price>, String> windowOf50,
			StreamStageWithKey<Entry<String, Price>, String> windowOf200) {

		// How to join
		AggregateOperation2<Entry<String, Price>, Entry<String, Price>, 
			MyPriceAccumulator, Tuple3<LocalDate, BigDecimal, BigDecimal>> 
			myAggregateOperation = MovingAverage.buildAggregateOperation();

		// Do the join
		StreamStage<KeyedWindowResult<String, Tuple3<LocalDate, BigDecimal, BigDecimal>>>
			joined = windowOf50
					.aggregate2(windowOf200,myAggregateOperation)
					.setName("joined");
		
		// Reformat to a Map.Entry
		return joined
				.map(
					(KeyedWindowResult<String, Tuple3<LocalDate, BigDecimal, BigDecimal>> entry) 
					->
					new SimpleImmutableEntry<String, Tuple3<LocalDate, BigDecimal, BigDecimal>>
						(MyConstants.BTCUSD, entry.getValue()))
				.setName("reformat")
				.groupingKey(Functions.entryKey());
	}

	
	/**
	 * <p>Create a bespoke sink that publishes whatever it gets
	 * as input to a IMDG {@link com.hazelcast.core.ITopic ITopic}
	 * </p>
	 *
	 * @return A sink to publish out data to a topic
	 */
	protected static Sink<? super Entry<?, ?>> buildAlertSink() {
		return SinkBuilder.sinkBuilder(
				"topicSink-" + MyConstants.ITOPIC_NAME_ALERT, 
				context -> context.jetInstance().getHazelcastInstance().getTopic(MyConstants.ITOPIC_NAME_ALERT)
				)
				.receiveFn((iTopic, item) -> iTopic.publish(item))
				.build();
	}
	
}
