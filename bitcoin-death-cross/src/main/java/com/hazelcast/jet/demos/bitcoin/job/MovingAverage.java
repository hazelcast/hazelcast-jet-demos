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
 * averages and detect when they cross. This is invoked
 * by {@link Task1}.
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
 *                         +-------------------+
 *                         |     priceFeed     |
 *                         |  averageOf1-noop  |
 *                         |    averageOf50    |
 *                         |    averageOf200   |
 *                         |      logSink      |
 *                         | mapSink-50 Point  |
 *                         | mapSink-200 Point |
 *                         |  mapSink-Current  |
 *                         |    streamOf50     |
 *                         |    streamOf200    |
 *                         |      joined       |
 *                         |     reformat      |
 *                         |   crossEmitter    |
 *                         |  topicSink-alert  |
 *                         +-------------------+
 * +-------------------+   +-------------------+   +-------------------+
 * +-------------------+   +-------------------+   +-------------------+
 * XXX Diagram goes here
 * </pre>
 * <p>What each step does is as follows:
 * </p>
 * <ol>
 * <li><p><b>priceFeed</b> : {@link Task4} writes Bitcoin / US Dollar
 * prices into an {@code com.hazelcast.core.IMap IMap}. Each new price
 * replaces the previous, so what is stored in Hazelcast in the
 * "{@code prices-in}" map is the current price.
 * </p>
 * <p>However, Hazelcast is configured (see "{@code hazelcast.xml}") to
 * be able to create an input stream of <em>changes</em>. Each time the
 * entry is updated in the map, Hazelcast stores the new value and
 * produces an event on this infinite stream.
 * </p>
 * <p>Each price in the input has a date, but we ignore this date on
 * the input and regard the stream as having no timestamps as we
 * don't need this timestamp yet and when we derive a value from
 * this input it will have a different timestamp.
 * </li>
 * <li><p><b>averageOf50</b> : Produce a moving average of the last
 * <em>50</em> prices. Each input is added to a ring-buffer of size
 * 50. Once the ring-buffer is full, add the 50 prices up, divide
 * by 50 and produce the first output item. Thereafter each item
 * of input results in one item of output.
 * </p>
 * <p>If the ring-buffer was size 3, the first item of output
 * would be the average of input items 1, 2 and 3. The second item
 * of output would be the average of items 2, 3 and 4.
 * <p>
 * </li>
 * <li><p><b>averageOf200</b> : This is the same as the "{@code averageOf50}"
 * stage, except the parameter is 200 to produce the moving average of
 * the last 200 prices.
 * </p>
 * </li>
 * <li><p><b>averageOf1-noop</b> : Calculate the moving average of
 * using a ring-buffer size of 1. Mathematically this does nothing
 * (a "{@code no-op}"), the average of one number is that number. We
 * use it here as a convenience to reformat the raw input into the
 * same format as produced by the "{@code averageOf50}" and
 * "{@code averageOf200}" streams so we can treat all three the
 * same later on.
 * </p>
 * </li>
 * <li><p><b>logSink</b> : <i>[Optional] : </i> Write the output
 * of the "{@code averageOf1-noop}" stage to the console. This
 * is just so we can see what is happening with the prices, to
 * aid understanding.
 * </p>
 * </li>
 * <li><p><b>mapSink-50 Point</b> : Save the "{@link averageOf50}"
 * stream to an {@link com.hazelcast.core.IMap IMap} named 
 * "{@code BTCUSD}". {@link Task2} listens to this map to capture
 * the values to plot on the graph. Each value replaces the previous
 * in the map, so this map holds the most recent 50-point average.
 * </p>
 * </li>
 * <li><p><b>mapSink-200 Point</b> : Save the stream from
 * "{@code averageOf200}" into the same map as we save the
 * "{@code averageOf50}". So {@link Task2} can track this value
 * too to plot on the graph, and this map stores the last
 * 200-point average.
 * </p>
 * </li>
 * <li><p><b>mapSink-Current</b> : Save the stream of unaltered
 * prices into the map named "{@code BTCUSD}". This is primarily
 * for {@link Task2}.</p>
 * <p>So map "{@code BTCUSD}" holds the latest values for the
 * 50-point average, 200-point average and unaltered original
 * price conveniently together. The unaltered price came from
 * and still exists in another map "{@code prices-in}" so
 * this is minor waste of space to duplicate it.
 * </p>
 * </li>
 * <li><p><b>streamOf50</b> : prepare the "{@code averageOf50}"
 * stream for joining.
 * </p>
 * <p>Firstly, add a timestamp. For the 50-point average we
 * could take the date of the first of the 50-points, the last
 * of the 50-points or the middle. All would have a sense to
 * them. We take the date of the last of the 50-points.
 * </p>
 * <p>Apply a window to this stream so Jet will control the release
 * of values to the next stage. We chose a tumbling window as
 * we don't want the next stage to see each average more than
 * once. We advance the window in 1-day units, and since we have
 * 1 average per day, this means our window contains at most one
 * value.
 * </p>
 * </li>
 * <li><p><b>streamOf200</b> : prepare the "{@code averageOf200}"
 * stream for joining by adding a timestamp to join on. Again,
 * use the end date of the average range as the timestamp.
 * </p>
 * </li>
 * <li><p><b>joined</b> : The join logic is easy! One input is
 * the 50-point average and the other is the 200-point average.
 * Each input has a timestamp and Jet arranges to provide us the
 * values one at a time.
 * </p>
 * <p>So the join boils down to creating a trio of values.
 * One of the trio is the price from the 50-point average input.
 * Another of the trio is the price from 200-point average input.
 * The last of the trio is the date, which is the same on both
 * 50-point and 200-point, but we take the 50-point one.
 * </p>
 * </li>
 * <li><p><b>reformat</b> : Convert the output of the "{@code joined}"
 * stage into a {@code Map.Entry}.
 * </p>
 * </li>
 * <li><p><b>crossEmitter</b> : Look for a cross on the 50-point
 * and 200-point averages.
 * </p>
 * <p>The cross definition is easy. From one day to the next the
 * 50-point has to move from a higher value than the 200-point
 * to being a lower value. <u>Or</u> from one day to the next the
 * 50-point has move from being a lower value than the 200-point
 * to being a higher value.
 * </p>
 * <p>The detail is that this stage keeps a copy of yesterday's
 * pair of 50-point and 200-point to compare against today's. 
 * </p>
 * </li>
 * <li><p><b>topicSink-alert</b> : The "{@code crossEmitter}" stage
 * only produces output if a cross is detected. If anything gets to
 * this stage, send it to a {@link com.hazelcast.core.ITopic ITopic}
 * so that {@link Task3} which is subscribed to the topic is aware.
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
		 *XXX
		averageOf1
			.drainTo(Sinks.logger())
			.setName("logSink");
		*/
		
		/** <p>Save the latest for each average to an
		 * {@link com.hazelcast.core.IMap IMap} for {@link Task2}.</p>
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

		/** <p>Create a timestamped stream from the 50-point averages.
		 * Add a window that advances it 1 day, and therefore 1 point,
		 * at a time.
		 * </p>
		 */
		StageWithKeyAndWindow<Entry<String, Price>, String> timestampedOf50 =
				MovingAverage.buildKeyedTimestamped(averageOf50, 50)
				.window(ONE_DAY_WINDOW);
		
		/** <p>Create a timestamped stream from the 200-point averages.
		 * </p>
		 */
		StreamStageWithKey<Entry<String, Price>, String> timestampedOf200 =
				MovingAverage.buildKeyedTimestamped(averageOf200, 200);
		
		/** <p>Join the timestamped 50-point and 200-point streams by
		 * the timestamped. Produces a trio of the date, and the
		 * price from each input for the matching date.
		 * </p>
		 */
		StreamStageWithKey<SimpleImmutableEntry<String, Tuple3<LocalDate, BigDecimal, BigDecimal>>, String> 
			joined50point200point
			= MovingAverage.join(timestampedOf50,timestampedOf200);
			
		/** <p>Business logic!</p>
		 * <p>Compare the trio of date, 50-point and 200-point in the
		 * stream to the previous, looking for price inflection.
		 * </p>
		 */
		StreamStage<Entry<?,?>> alerts =
			joined50point200point
			.customTransform("crossEmitter", CrossEmitter::new);

		/** <p>If there is anything produced by the {@link CrossEmitter}
		 * dump it to a {@link com.hazelcast.core.ITopic ITopic} for
		 * {@link Task3}. What comes out is a {@code Map.Entry} so we could
		 * easily dump it to an {@link com.hazelcast.core.IMap IMap} instead
		 * (or as well) and use a map listener.
		 * </p>
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
	 * stream of prices, and create a timestamp on that stream. Based the
	 * timestamp on the last date of the average.
	 * </p>
	 *
	 * @param averageOfSomething Average of 50 points or the average of 200 points
	 * @param count For building the stage name
	 * @return A timestamped stream on this input
	 */
	private static StreamStageWithKey<Entry<String, Price>, String> 
		buildKeyedTimestamped(StreamStage<Entry<String, Price>> averageOfSomething, int count) {

		return averageOfSomething
				.addTimestamps(e -> e.getValue().getTimestamp(), ZERO_LAG)
				.setName("streamOf" + count)
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
