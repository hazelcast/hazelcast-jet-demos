package com.hazelcast.jet.demos.market.data.analyzer;

import java.util.HashMap;

import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class ApplicationConfig {

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate(@Value("${bootstrap-servers}") String bootstrapServers) {
		Map<String, Object> producerConfigs = new HashMap<>();
		
		producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		producerConfigs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerConfigs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

		ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerConfigs);

		KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory);
				
		kafkaTemplate.setDefaultTopic(Constants.TOPIC_NAME_PRECIOUS);
		
		return kafkaTemplate;
	}
}