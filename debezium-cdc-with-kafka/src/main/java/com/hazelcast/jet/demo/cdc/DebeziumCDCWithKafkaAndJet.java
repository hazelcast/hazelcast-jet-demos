package com.hazelcast.jet.demo.cdc;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.impl.JetBootstrap;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import org.apache.kafka.connect.json.JsonDeserializer;

import java.util.Properties;

/**
 * Simple Jet pipeline which consumes CDC events originated from MySQL via Debezium
 * from Kafka, prints the objects to the standard output in the string format and
 * writes them to a Hazelcast IMap..
 */
public class DebeziumCDCWithKafkaAndJet {

    public static void main(String[] args) {
        JetInstance jet = JetBootstrap.getInstance();

        Properties properties = new Properties();
        properties.setProperty("group.id", "cdc-demo");
        properties.setProperty("bootstrap.servers", "kafka:9092");
        properties.setProperty("key.deserializer", JsonDeserializer.class.getCanonicalName());
        properties.setProperty("value.deserializer", JsonDeserializer.class.getCanonicalName());
        properties.setProperty("auto.offset.reset", "earliest");
        Pipeline p = Pipeline.create();

        p.readFrom(KafkaSources.kafka(properties, record -> {
            HazelcastJsonValue key = new HazelcastJsonValue(record.key().toString());
            HazelcastJsonValue value = new HazelcastJsonValue(record.value().toString());
            return Util.entry(key, value);
        }, "dbserver1.inventory.customers"))
         .withoutTimestamps()
         .peek()
         .writeTo(Sinks.map("customers"));

        jet.newJob(p).join();
    }
}
