package com.hazelcast.jet.demos.market.data.ingest;

import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

public class ReadKafkaIntoHazelcast {

    public static Pipeline build(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Pipeline pipeline = Pipeline.create();

        pipeline
                .drawFrom(KafkaSources.kafka(properties, Constants.TOPIC_NAME_PRECIOUS))
                .withoutTimestamps()
                .drainTo(Sinks.map(Constants.IMAP_NAME_PRECIOUS));

        return pipeline;
    }

}
