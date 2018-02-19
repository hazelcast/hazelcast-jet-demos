# Market Data Analyzer

This is an interactive demo showing both how Jet can be used to easily import data into Hazelcast IMDG,
and to analyse the data as it changes.

## Market Data

"_Market Data_" is a term normally associated with the stock markets. These markets are external both to the
application, and usually also to the organisation.

In the stock market, there are various markets for various things - bonds, equities, currencies, etc. The market
data itself is information about activities, the prices items are buying and selling for, volumes being
traded, and so on.

As an organisation, we would usually _pay_ for a real-time feed of this market data, and wish to make the
best value for money from the information.

This market data feed will arrive via some external interface into our organisation, and would be highly
unlikely to be in the format that suits us best. A bit of reformatting might be required.

### Precious Metals

To keep this example simple, we are going to focus on the market for precious metals. The big four
are Gold, Silver, Platinum and Palladium. The first three are used in jewelry, with Palladium being
a key component in the catalytic converters for combustion engine vehicles such as cars.

Furthermore, we shall simplify to hold a current price for each of these four. The reality would
be to hold several prices for each commodity, a _buy_ price, a _sell_ price, daily highs, daily
lows, opening prices, closing prices, and perhaps others.

## The application

The application here consists of three parts.

[Apache Kafka](https://kafka.apache.org/) is used as the market data feed, acting as the transport
for the precious metal prices from the outside world to our application. Kafka is picked here as a
popular transport mechanism, but anything else would be fine also.

There is a `kafka-writer` module that generates random market data and writes it in to Kafka.
This similates the real market data feed. If you have a real market data feed, you can extend
the application to use that instead.

Finally, there is a Hazelcast grid, a collection of In-Memory Data Grid [IMDG](https://hazelcast.org/)
processes each of which embeds a [Jet](https://jet.hazelcast.org/) processing engine. Embedding
Jet in the same process as IMDG is the easiest way to connect the two rather than have them
in separate JVMs.

## Running the example

This is an interactive example, the JVMs hosting IMDG & Jet respond to commands from the command
line.

The sample output below assumes there is one Zookeeper process, three Kafka processes, and
three IMDG/Jet processes.

### Kafka

This example assumes that Kafka is running on the current machine, and is controlled
via some provided scripts.

Kafka 1.0.0, released 1st November 2017, is the current stable version at the time of writing,
and can be obtained from [here](https://kafka.apache.org/downloads). The version used is
for Scala 2.11 not 2.12, as the former is more widespread.

`KAFKA_HOME` environment variable should be set to where Kafka is installed. 
You can use the command  `export KAFKA_HOME=<install dir>` to set the variable. 
There are five command scripts in `src/main/scripts` to run in this sequence.

1. `start-zookeeper.sh` 
This script will start a single Zookeeper configuration server, running on port 2181 on the current machine.

1. `start-kafka-0.sh`
This script will start a first Kafka broker, running on port 9092 on the current machine.

1. `start-kafka-1.sh`
This script will start a second Kafka broker, running on port 9093 on the current machine.

1. `start-kafka-2.sh`
This script will start a third Kafka broker, running on port 9094 on the current machine.

1. `print-topic.sh`
This script will first create a partitioned Kafka topic called "_precious_" if this does not
exist. Then it will listen for data being written to this topic and echo it to the screen.
It will not stop until interrupted with _Ctrl-C_.

Some other scripts are also provided

* `clean-topic.sh` This will shut down Kafka and Zookeeper, and remove data from the topics.

* `stop-kafka.sh` This will stop the Kafka brokers but leave Zookeeper running.

* `stop-zookeeper.sh` This will stop Zookeeper, if this is running and Kafka brokers are stopped.

#### Kafka : Example Output

Assuming Zookeeper and the three Kafka brokers have started successfully, the `print-topic.sh`
script might produce output like this:

```
$ ./print-topic.sh 
================
Topic name : precious
================
./kafka-topics.sh --zookeeper 127.0.0.1:2181 --list --topic precious
RC=0
./kafka-topics.sh --zookeeper 127.0.0.1:2181 --create --partitions 3 --replication-factor 1 --topic precious
Created topic "precious".
RC=0
Waiting....
Done.
./kafka-topics.sh --zookeeper 127.0.0.1:2181 --describe --topic precious
Topic:precious	PartitionCount:3	ReplicationFactor:1	Configs:
	Topic: precious	Partition: 0	Leader: 1	Replicas: 1	Isr: 1
	Topic: precious	Partition: 1	Leader: 2	Replicas: 2	Isr: 2
	Topic: precious	Partition: 2	Leader: 0	Replicas: 0	Isr: 0
RC=0
^C to cancel...
./kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic precious --from-beginning

```

The topic shouldn't exist at the first run, so the topic ("_precious_") is created. The creation
command specifies to create it with three partitions.

At this stage, the topic is empty, nothing has been written to it.

Keep this script running, as confirmation for the next step.

### Kafka Writer

The next step is to write some data into Kafka.

This uses the `kafka-writer` module which generates test data. Run this with:

```
java -jar kafka-writer/target/kafka-writer-0.1-SNAPSHOT.jar
```

This generates 50 random prices, and produces output of what it is writing to Kafka. 

You might see lines such as.

```
16:34:55.203 INFO  kafka-producer-network-thread | producer-1
				c.h.j.d.m.d.analyzer.TestDataGenerator - onSuccess(), offset 10 partition 1 timestamp 1518885294981 for 'Palladium'=='0.9119393021705703' 
16:34:55.203 INFO  kafka-producer-network-thread | producer-1
				c.h.j.d.m.d.analyzer.TestDataGenerator - onSuccess(), offset 5 partition 2 timestamp 1518885295175 for 'Platinum'=='0.4450432122585468' 
16:34:55.204 INFO  kafka-producer-network-thread | producer-1
				c.h.j.d.m.d.analyzer.TestDataGenerator - onSuccess(), offset 22 partition 0 timestamp 1518885295174 for 'Gold'=='0.011997721612340806' 
16:34:55.204 INFO  kafka-producer-network-thread | producer-1
				c.h.j.d.m.d.analyzer.TestDataGenerator - onSuccess(), offset 23 partition 0 timestamp 1518885295174 for 'Gold'=='0.7147764727476067' 
```

The above 4 lines from `kafka-writer` show a sequence of prices generated. A price is generator for Palladium, then
for Platinum, then for Gold, then for Gold again. 

Note though the _partition_ for each line, this is very important.

The writer is parallelising the writes across the partitions in a consistent manner. The write of the Platinum price
goes to partition 2. The write of the Gold price goes to partition 0. The write of the _next_ Gold price goes
to partition 0. So Gold prices are sequentially written and so can be sequentially read.

There are four metals and three partitions. Each metal always is written to the same partition, but it may have
to share that partition with the prices of another metal.  

### Hazelcast

The next thing to start is the Hazelcast grid. Run this command:

```
java -jar hazelcast-grid/target/hazelcast-grid-0.1-SNAPSHOT.jar
```

and allow the processes to get as far as the command prompt, indicating they are up.

This example runs 5 such Hazelcast processes, and gives this output as they join together to form a grid.

```
16:44:55.701 INFO  hz._hzInstance_1_market-data-analyzer.priority-generic-operation.thread-0
				c.h.internal.cluster.ClusterService - [127.0.0.1]:5701 [market-data-analyzer] [0.6-SNAPSHOT] 

Members {size:5, ver:2} [
	Member [127.0.0.1]:5701 - 85b78703-77a5-4dd1-9bc3-7168eb5dbb82 this
	Member [127.0.0.1]:5702 - 10fbe674-305d-4aac-8eb1-4503e5410eb7
	Member [127.0.0.1]:5703 - 93add205-d33c-44e9-a5cc-a212c1cd9a30
	Member [127.0.0.1]:5704 - 076903b1-ef78-4379-91d2-7d25c194b6eb
	Member [127.0.0.1]:5705 - f93ab33f-ea1c-4663-b20b-6915fcf7fcd7
]
 

'Market Data Analyzer' cluster $
```

Any number of processes in the grid will do, whatever your machine can cope with.

#### Hazelcast 1 - "_LIST_"

The first command to try from any of the Hazelcast processes is "_LIST_".

```
'Market Data Analyzer' cluster $ LIST

IMap: 'command'
[0 entries]

IMap: 'precious'
[0 entries]

```

This command will list the Hazelcast storage structures, known as 
[IMap](http://docs.hazelcast.org/docs/3.9.3/javadoc/com/hazelcast/core/IMap.html). These
are akin to [java.util.Map](https://docs.oracle.com/javase/8/docs/api/java/util/Map.html)
except they are partitioned across the processes in the Hazelcast grid.

There are two such maps, named "_command_" and "_precious_" and both are currently empty.

#### Hazelcast 2 - "_KAFKA_"

The next command to run is "_KAFKA_" to start the Jet job to upload from the Kafka topic
into Hazelcast.

This job runs continuously reading from the Kafka "_precious_" topic and writing into
Hazelcast's "_precious_" map. As we have already put data in the Kafka topic with the
`kafka-writer` module, the Jet job has data to process and will start processing it
immediately.

The Kafka topic is partitioned into 3. The Hazelcast grid is also composed of several
processes, which for the worked example was 5 processes.

So, three streams of Kafka data are read into 5 Hazelcast processes in parallel.

This means two of the Hazelcast processes won't have a Kafka partition to read
and three will. Each of the Hazelcast processes will log what it is doing,
which is something for three and nothing for two.

For example, this comes from one of them:

```
16:50:47.251 INFO  hz._hzInstance_1_market-data-analyzer.event-1
				c.h.j.d.m.data.analyzer.LoggingListener - UPDATED event, map 'precious', key 'Silver', new value '0.8646005761659097' 
16:50:47.262 INFO  hz._hzInstance_1_market-data-analyzer.event-1
				c.h.j.d.m.data.analyzer.LoggingListener - UPDATED event, map 'precious', key 'Silver', new value '0.1578087015086006' 
```

This process is uploading the prices for Silver, and writing it into a Hazelcast map.

This generates "_UPDATED_" events, as each price uploaded replaces the predecessor.

#### Hazelcast 3 - "_LIST_"

Now run the "_LIST_" command from any of the Hazelcast servers.

Output might look like:

```
'Market Data Analyzer' cluster $ LIST

IMap: 'command'
    -> 'kafka' -> [start, 127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094]
[1 entry]

IMap: 'precious'
    -> 'Gold' -> 0.09187902227340827
    -> 'Palladium' -> 0.3137210129760756
    -> 'Platinum' -> 0.4450432122585468
    -> 'Silver' -> 0.1578087015086006
[4 entries]

```

There are still two maps, and both contain data.

##### "_precious_"

The "_precious_" map contains four entries, one for each of the precious metals. For each, there is
a price, the list price uploaded, as each price uploaded for a metal replaces the previous value.

What we have available in the IMDG map is the current price.

##### "_command_"

The "_command_" map is how this example controls Jet, using the command [Command Pattern](https://en.wikipedia.org/wiki/Command_pattern).

When a Jet job is required to be controlled, a command is written to the "_command_" map with the details.

In this case, the command is for the "_kafka_" job to be started with the parameter list for the Kafka
brokers to connect to.

A map listener on the IMDG will observe this command and may act on it. It won't start the Kafka job twice.

You can try this, by entering "_KAFKA_" to the command prompt. One of the IMDG instances will pick up
the command request and log that it is ignoring it:

```
17:03:12.557 INFO  hz._hzInstance_1_market-data-analyzer.event-4
				c.h.j.d.m.data.analyzer.CommandListener - Ignoring start request, Kafka Reader job id 8664015112292097692 already running 
```

#### Hazelcast 4 - "_HISTORY_"

The last command to run from Hazelcast command prompt is "_HISTORY_".

This will request that the Jet job to analyse the price history is run.

Again, the output from this job will be spread across the Hazelcast processes, and some may have no data
to output as the analysis is parallelised and the data is parallelised.

You might see something like this from the Hazelcast instance handling Platinum.	

```
17:06:13.663 INFO  hz._hzInstance_1_market-data-analyzer.jet.blocking.thread-5
				c.h.j.i.c.WriteLoggerP.loggerSink#3 - [127.0.0.1]:5703 [market-data-analyzer] [0.6-SNAPSHOT] Platinum==0.8267103140203242 
17:06:13.663 INFO  hz._hzInstance_1_market-data-analyzer.jet.blocking.thread-5
				c.h.j.i.c.WriteLoggerP.loggerSink#3 - [127.0.0.1]:5703 [market-data-analyzer] [0.6-SNAPSHOT] Platinum==0.4163019335110366 
17:06:13.663 INFO  hz._hzInstance_1_market-data-analyzer.jet.blocking.thread-5
				c.h.j.i.c.WriteLoggerP.loggerSink#3 - [127.0.0.1]:5703 [market-data-analyzer] [0.6-SNAPSHOT] Platinum==0.4450432122585468 
```

You'll see something similar from the instance handling Palladium. Gold and Silver are suppressed.	

##### Map Journal

The Jet history job relies on IMDG's [Event Journal](http://docs.hazelcast.org/docs/3.9.3/javadoc/com/hazelcast/journal/EventJournal.html).

In the "_hazelcast.xml_" file that configures the IMDG server, there is this section:

```
	<event-journal enabled="true">
		<mapName>precious</mapName>
		<capacity>10000</capacity>
		<time-to-live-seconds>0</time-to-live-seconds>
	</event-journal>
```

This means we have asked Hazelcast to keep a rolling history of the last 10,000 changes to data in
the "_precious_" map forever.

This data structure is not a replacement for the map eventing system used by [Entry Listener](http://docs.hazelcast.org/docs/3.9.3/javadoc/com/hazelcast/core/EntryListener.html), it is complementary.

Firstly, it is a history. You can access events as far back as configured (here 10,000). A map entry listener only
gets events that occur after it has been added. This is obvious here as we started the "_HISTORY_" command after
the "_KAFKA_" command had been seen to upload any data. 
	
Secondly, it gives access to multiple items. A map entry listener is called once per event, it does not have
access to the previous event.

##### Enhancements Possible

This example doesn't really do anything significant with the stream of prices. Gold and Silver are filtered
out, Platinum and Palladium are logged to the system output.

The map Event Journal is used for this for illustrative purposes.

However, the Event Journal is essentially post-processing, a configurably sized recent history of changes
to the map.

It would be more effective to move this from post-processing to pre-processing, to merge the logic
of the "_History_" Jet job into the "_Kafka_" Jet job.

This is not done in this example for clarity, but would be a good enhancement for you to try.

After all, it is wasteful to keep a history of *changes* to Gold and Silver prices if we are not
going to use them. Better to filter out before storage.

### Variations

In the above instructions, the `kafka-writer` is run before the Hazelcast IMDG grid is started, so
the data is waiting to be processed. This is hardly real-time.

If you like, you can repeat this in the reverse sequence (but run `clean-topic.sh` first). 

Start Hazelcast IMDG first, initiate the Jet upload job. Nothing will happen as Jet is reading from
Kafka real-time and there is no data. Then run `kafka-writer` to start writing and you'll see
Hazelcast process it immediately.

## A deeper look at Jet

There are two Jet jobs in this example.

### `ReadKafkaIntoHazelcast.java`

The code for this is mostly this:

```
        Pipeline pipeline = Pipeline.create();
        
        pipeline
        .drawFrom(KafkaSources.kafka(properties, noWatermarks(), Constants.TOPIC_NAME_PRECIOUS))
        .drainTo(Sinks.map(Constants.IMAP_NAME_PRECIOUS));
        ;
        
        return pipeline;
```

That's it. Two lines do the work, a data source and a data sink.

The `drawFrom` line tells Jet that the source is Kafka. We pass it properties to connect to Kafka,
that we don't want watermarks (timestamps) interspersed automatically by Jet, and the name of
the topic we wish to read.

The `drainTo` line tells Jet that the output goes to a Hazelcast map called "_precious_".

This is very easy but also very powerful.

The above defines a processing pipeline that runs in parallel across the Hazelcast grid.

Five Hazelcast instances can use a Jet job to ingest data at a very great speed. If
higher speeds are needed, just increase the cluster size to ten and the ingest rate
doubles. It's that simple.

#### Properties

The properties in the above are standard `java.util.Properties` for Kafka connection:

```
        Properties properties = new Properties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
```

The main parts being the list of Kafka brokers (`bootstrapServers`) and that the key and value read from Kafka
are Strings.

Although the price should be a number, it is easier here to pass it as a string so we don't have to handle
parsing. This would not be difficult to add.

### `PreciousHistory.java`

The code for this is mostly this:

```
        Pipeline pipeline = Pipeline.create();
        
        pipeline
        .drawFrom(Sources.<String, Object>mapJournal(Constants.IMAP_NAME_PRECIOUS, JournalInitialPosition.START_FROM_OLDEST, noWatermarks()))
        .map(entry
        		-> 
        		new String(entry.getKey() + "==" + entry.getValue())
        	)
        .filter(str -> str.toLowerCase().startsWith("p"))
        .drainTo(Sinks.logger())
        ;
        
        return pipeline;
```

This is a fractionally more complicated data processing pipeline than the previous one, it has four steps.

The `drawfrom` step takes the Hazelcast IMDG map journal as input. This data structure is partitioned
across the Hazelcast processes. This step has the option to start from current point in time, but
instead is coded to go back into history as far as is recorded. So we can analyse events that have
already occurred.

The `map` stage takes the entry event from the change history, and reformats the key and value
into a single String.

The `filter` stage only allows strings beginning with "_p_" to pass. Hence Gold and Silver are
excluded, Platinum and Palladium pass through.

Finally, the 'drainTo` stage completes the pipeline by sending the derived data somewhere.
In this case, the somewhere is a logging routine. Each Hazelcast process logs the data is
alone has processed, logging is parallelised. For real usage, storage somewhere is probably
going to be more relevant.

Again, the power here is both in the simplicity and the parallelisation. The coding is easy, but
scaling of Hazelcast makes it run efficiently.

#### Did you spot the mistake ?

Well, it's not really a big mistake, but step 3 is a filter and step 2 is formatting. It would be more
efficient to filter before re-formatting. As it stands currently, we are re-formatting data which
we might then discard.

## Summary

Kafka is partitioned, Hazelcast is partitioned. Ingest from Kafka to Hazelcast is both easy coding
and efficient coding. Jet upload from Kafka runs in parallel, each node reads from some Kafka
partitions, minimising data cross-transfer on the network.

Jet can merge data into IMDG data structures, here [IMap](http://docs.hazelcast.org/docs/3.9.3/javadoc/com/hazelcast/core/IMap.html)
is used. This writing to IMDG can operate in parallel across the grid, minimising data cross-transfer on the network.

Traditional IMDG time-series analysis requires the input to be stored. Time-series analysis with Jet
operates on a memory stream, it is the output that is stored. Reducing the storage need for intermediate
data gives faster calculations and cost savings.