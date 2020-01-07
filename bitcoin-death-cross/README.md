# bitcoin-death-cross

<a href="https://github.com/hazelcast/hazelcast-jet-demos/tree/master/bitcoin-death-cross">This example</a> shows how Jet is used to spot the dramatically-named <a href="https://en.wikipedia.org/wiki/Moving_average_crossover">Death Cross</a> for the price of Bitcoin, which is an indication to <i>sell</i>, <i>Sell</i>, <i>SELL!</i>.

The idea here is that we could automatically analyze stock market prices and use this information to guide our buying and selling decisions.

<img src="https://raw.githubusercontent.com/hazelcast/hazelcast-jet-demos/master/bitcoin-death-cross/src/site/markdown/images/Screenshot1.png" alt="Image of Bitcoin prices in 2017 and 2018" />
<h2>Disclaimer</h2>
This is a code example, <b>not trading advice</b>. Do not use it to make investment decisions. Please!
<h2>Background</h2>
In the graph at the top of this post, the price of Bitcoin is shown as a <span style="color: blue;">blue line</span>.

The price goes up and down, and to the human eye, it's relatively clear that it peaked in December 2017.

So selling in December 2017 might not have been a bad idea, because by January 2018, the price had fallen. But then in February 2018, the price went back up again, so we might have regretted selling.

To confidently buy/sell Bitcoin, we need to study the overall price trend. Is the price going up or down, in general, rather than from moment to moment?
<h3>Moving Averages</h3>
What we calculate are moving averages to smooth out the variation in the price.

The <span style="color: red;">red line</span> is the "<i>50-Point Moving Average.</i>"

The idea for the 50-point moving average is to take 50 input points and calculate the average, then move 1 point forward and repeat.

We have daily prices, so the first 50 points to average are January 1st, 2017 to February 19th, 2017. The average of these 50 prices is $951.

The next 50 points to average are January 2nd, 2017 to February 20th, 2017. We have moved forward by one input. 49 of the 50 points considered are the same as the previous calculation, so the average will not vary much. This smoothes out variation in the input, giving the output on February 20th, 2017 of $952.

The price of Bitcoin from February 19th-20th, 2017 was a move from $1048 to $1077. The 50 point average has gone from $951 to $952, recognizing an upward trend but a reduction in volatility.

The <span style="color: purple;">purple line</span> is the "<i>200-Point Moving Average.</i>" The idea is the same, except it uses 200 points, so it doesn't begin producing output until July 19th, 2017.

50 and 200 are standard ranges used in investment. The 50-point range gives the short-term trend, while the 200-point range gives the long-term trend.
<h3>Death Cross and Golden Cross</h3>
What we are looking for is whether the short-term trend and the long-term trend are going in different directions.

On March 30th, 2018, the short-term trend dipped below the long-term trend. This is called the Death Cross.

Later on, in April 2019, they cross the other way, known as the Golden Cross.

Golden Cross and Death Cross indicate positive and negative perspectives on the price. For a trader, both situations means there is money to be made, so both are good news.
<h2>What Does Jet Add?</h2>
Speed!

The business logic here is just averages; nothing remarkable. The value comes from speed.

We need to detect it in time to sell when others still think buying is a good idea. After all, we can only sell to someone that wants to buy.

If we do this on the price stream as it comes in, we're going to be faster than those that store the prices to analyze later. Time is money.
<h2>Running the Example</h2>
From the top-level of the <code>bitcoin-death-cross</code> folder, use:
<pre><code>
mvn spring-boot:run
</code></pre>
Or, if you prefer:
<pre><code>
mvn install
java -jar target/bitcoin-death-cross.jar
</code></pre>
What you should see is a graph appear and the prices plotting dynamically.

The prices themselves are stored in the <code>src/main/resources/BTCUSD.csv</code> file, but you could easily convert to connect to a live stock exchange if you wanted.
<h2>Understanding the Example</h2>
<h3>Business Logic</h3>
The business logic here is based around trend analysis using the "simple moving average."

For one 50-point calculation, 50 consecutive prices are added together, and the sum is divided by 50 to give the result. A calculation is made from points 1 to 50, then from 2 to 51, then from 3 to 52, and so on.

But there are plenty of ways we could make the calculation more sophisticated, in the hope of providing better results.

For example, using points 1 to 50 to produce a value, then points 2 to 51 to deliver the next value provides minimal variance. Both calculations use points 2 to 50; only points 1 and 51 differ,  so the output does not vary by much. We might decide to alter the stepping factor from 1 to 2 so there is less overlap when using points 1 to 50, then points 3 to 52, then 5 to 54, then 7 to 56, and so on.

We could also introduce a weighting to reflect the fact that newer prices are more important. We might count the last point twice, for example. Here we would add points 1 to 50 together as before, add point 50 again, and now divide by 51 to get our result. This would no longer genuinely be considered an average.
<h3>Map Journal</h3>
Hazelcast provides an <a href="https://docs.hazelcast.org/docs/3.12.5/manual/html-single/index.html#event-journal">Event Journal</a> on map and cache data structures.

Our price feed is preset, as it's just an example. The class <code>com.hazelcast.jet.demos.bitcoin.Task4PriceFeed</code> reads from the CSV file of prices and writes them into a map.

So what the map stores is the current price of Bitcoin. If we do "<code>map.get("BTCUSD")</code>" we get back a single value—the last value written, as you would expect.

But the <code>hazelcast.yaml</code> configuration file tells Hazelcast to track the history of changes to this map, and Jet can process this event log. So now we have access to every value written for each key, depending on the capacity we configure for the event log.

The Jet code to do this is below:
<pre><code>
pipeline.drawFrom(
  Sources.mapJournal(
    MyConstants.IMAP_NAME_PRICES_IN,
    JournalInitialPosition.START_FROM_OLDEST)
)
</code></pre>
We tell Jet to use the change history for the given map as an infinite input stream. Jet has access to events from before the Jet job started executing (<code>START_FROM_OLDEST</code>) and every update that occurred while the Jet job ran.

<a href="https://docs.hazelcast.org/docs/jet/3.2/javadoc/com/hazelcast/jet/pipeline/Sources.html#mapJournal-com.hazelcast.core.IMap-com.hazelcast.jet.pipeline.JournalInitialPosition-">Sources.mapJournal()</a> gives a continuous stream of <i>updates</i> to a map, unlike <a href="https://docs.hazelcast.org/docs/jet/3.2/javadoc/com/hazelcast/jet/pipeline/Sources.html#map-java.lang.String-">Source.map()</a>, which provides a one-off batch of current map content.

If you want to connect to a live stock exchange, the above is the element that needs to change.
<h3>The Pipeline</h3>
The pipeline for this demo is relatively large:

<img src="https://raw.githubusercontent.com/hazelcast/hazelcast-jet-demos/master/bitcoin-death-cross/src/site/markdown/images/Screenshot2.png" alt="Image of the entire job pipeline" />

However, that's because it's doing various things to make the demo more interesting, like saving the prices to a map so other code can plot the prices on a chart.

There are only four parts of specific interest:
<h4>Price Feed</h4>
<img src="https://raw.githubusercontent.com/hazelcast/hazelcast-jet-demos/master/bitcoin-death-cross/src/site/markdown/images/Screenshot3.png" alt="Image of the job source feeding three stages" />

The first stage of the pipeline is, of course, the source. In this case, it's an infinite stream of prices coming from a map event journal.

A recurring theme for stream processing is <i>reduction</i>. A feed of <u>big data</u> is transformed into meaningful <u>small data</u> by a series of filters, projections, and aggregations.

Here we do the reverse—we increase the data volume. Our single feed of prices broadcasts to three subsequent pipeline steps. For every one price that comes in, three copies of it are sent to later stages in the pipeline.

We do this to calculate the 50-point moving average and 200-point moving average from the same input data. As a bonus, we also calculate the 1-point moving average. The 1-point moving average is, of course, a <i>no-op</i>; each input price produces the same output price, which in this example is a helpful convenience to reformat the input into the output format.

So the code here is:
<pre><code>
StreamStage&lt;Entry&gt; averageOf1 =
    MovingAverage.buildAverageOfCount(1, priceFeed);
StreamStage&lt;Entry&gt; averageOf50 =
    MovingAverage.buildAverageOfCount(50, priceFeed);
StreamStage&lt;Entry&gt; averageOf200 =
    MovingAverage.buildAverageOfCount(200, priceFeed);
</code></pre>
<h4>Moving Average</h4>
To calculate the moving average itself, we provide a method to transform the input into the output.
<pre><code>
priceFeed.customTransform(
    stageName, () -&gt; new SimpleMovingAverage(count)
</code></pre>
This code uses a ring buffer to store the input prices. This is sized at 1, 50, or 200, depending on which average we are calculating.

For each item of input, we add it to the ring buffer, and if it is not full, we indicate to Jet (by returning <code>true</code>) that we have successfully processed the input.
<pre><code>
if (this.rates[next]==null) {
    this.current = next;
    return true;
}
</code></pre>
Once the ring buffer is full, we can create the average and <i>try</i> to produce the output. This can fail if the output queue for this step is full, so we use this to determine if we successfully processed the input.
<pre><code>
Entry result
    = new SimpleImmutableEntry(this.key, average);
                        
boolean emit = super.tryEmit(result);
if (emit) {
    this.current = next;
}
return emit;
</code></pre>
<h4>Join</h4>
From a technical standpoint, the most interesting stage is the join.

<img src="https://raw.githubusercontent.com/hazelcast/hazelcast-jet-demos/master/bitcoin-death-cross/src/site/markdown/images/Screenshot4.png" alt="Image of join step" />

The 50-point moving average stream is producing pairs of dates and prices. The 200-point moving average stream is also producing pairs of dates and prices.

What we want to do is join these on the date, to produce a trio—the date, the 50-point price, and the 200-point price. <span style="font-size: 1.125rem; letter-spacing: 0px; font-family: 'Akkurat Std', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Helvetica Neue', sans-serif;">So our input to the join is two streams.</span>
<pre><code>
StageWithKeyAndWindow&lt;Entry, String&gt; timestampedWindowedOf50;
StreamStageWithKey&lt;Entry, String&gt; timestampedOf200;
</code></pre>
The left side is the 50-point stream, which is timestamped from the date of the average, the 50th day for the 50-point. It advances in windows of one day, so we get one price at a time since we have daily prices.

The right side is the 200-point stream, which also is timestamped from the date of its average, the date of the last (200th) price.

We join them easily,
<pre><code>
windowOf50.aggregate2(windowOf200, myAggregateOperation)
</code></pre>
using a 2-stream aggregator. The left side <code>windowOf50</code> is blended with the right side <code>windowOf200</code> using a customer aggregation.

The aggregation has three main parts:
<pre><code>
public MyPriceAccumulator setLeft(Price arg0) {
    this.left = arg0.getRate();
    this.date = arg0.getLocalDate();
    return this;
}

public MyPriceAccumulator setRight(Price arg0) {
    this.right = arg0.getRate();
    return this;
}

public Tuple3 result() {
    return Tuple3.tuple3(this.date, this.left, this.right);
}
</code></pre>
When a left side item arrives, <code>setLeft()</code> is called, and this saves the date and the price from that item (50-point) to instance variables in the aggregator object.

When a right-side item arrives, <code>setRight()</code> is called, saving the price from the 200-point item. We could save the date here too, but it's the same date as came in on the left side.

Finally, once left and right items have been received, <code>result()</code> is called to make the output. <code>Tuple3</code> is a utility class provided by Jet for holding trios. There is also <code>Tuple4</code>, <code>Tuple5</code>, etc.
<h4>Alerting</h4>
The last stage is alerting; there's no use detecting a Death Cross or Golden Cross if we can't tell anyone about it.

We create our own sink, to publish to a Hazelcast topic. Any processes with topic listeners, such as Hazelcast clients, would be notified if the Jet job wrote anything to the topic.
<pre><code>
SinkBuilder.sinkBuilder(
    "topicSink-" + MyConstants.ITOPIC_NAME_ALERT, 
    context -&gt; 
        context.jetInstance().getHazelcastInstance().getTopic(MyConstants.ITOPIC_NAME_ALERT)
    )
    .receiveFn((iTopic, item) -&gt; iTopic.publish(item))
    .build();
</code></pre>
We use the execution context to retrieve a reference to the Hazelcast topic, and if this stage receives an item, it uses the object returned from the context as the place to publish the item.
<h2>Does This Work?</h2>
Alerts are produced for a downward cross on March 30th, 2018, an upward cross on April 24th, 2019, and another downward cross on October 26th, 2019.

So if you had one Bitcoin to begin with, and sold it for $6853 on March 30th, 2018, you would have $6,853.

$6,853 would have bought 1.26 Bitcoins on April 24th, 2019.

Selling 1.26 Bitcoins on October 26th, 2019 would turn this in $11,629.

If you'd held on to the 1 Bitcoin throughout the time period, by the end of 2019, this would be worth $7,196.

Bitcoin started 2017 at $995, so $7,196 is not bad, but $11,629 is better.
<h2>Summary</h2>
This example shows how you can derive insights on a stream of data as it arrives, which is vital for time-critical applications. The code is <a href="https://github.com/hazelcast/hazelcast-jet-demos/tree/master/bitcoin-death-cross">here</a>.

As a reminder, the business logic in the example is pretty flimsy. <b>Do not use it to make investment decisions.</b>

Price trend analysis is a real industry segment, and it just uses better rules.

In the future, we'll look at $995 US Dollars for 1 Bitcoin with as much amusement as 2005's $100 US Dollars for 1GB of RAM.
