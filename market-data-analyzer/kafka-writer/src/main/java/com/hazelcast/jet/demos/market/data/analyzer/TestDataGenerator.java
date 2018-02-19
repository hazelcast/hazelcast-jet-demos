package com.hazelcast.jet.demos.market.data.analyzer;

import java.util.Random;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
@Slf4j
public class TestDataGenerator implements CommandLineRunner {

	private static Random random = new Random();

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Override
	public void run(String... arg0) throws Exception {

		int MAX = 50;

		for (int i = 0; i < MAX; i++) {

			// Which metal
			int m = random.nextInt(Constants.PRECIOUS_METALS.length);
			String metal = Constants.PRECIOUS_METALS[m];

			// Price
			String price = random.nextDouble() + "";

			log.info("ADD {} {}", metal, price);
			
			// Kafka is partitioned
			int partition = m % 3;

			ListenableFuture<SendResult<String, String>> sendResult = this.kafkaTemplate.sendDefault(partition, metal,
					price);

			sendResult.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
				@Override
				public void onSuccess(SendResult<String, String> sendResult) {
					ProducerRecord<String, String> producerRecord = sendResult.getProducerRecord();
					RecordMetadata recordMetadata = sendResult.getRecordMetadata();
					log.info("onSuccess(), offset {} partition {} timestamp {} for '{}'=='{}'",
							recordMetadata.offset(),
							recordMetadata.partition(), recordMetadata.timestamp(), 
							producerRecord.key(), producerRecord.value());
				}

				@Override
				public void onFailure(Throwable t) {
					log.error("onFailure()", t);
				}
			});
		}

	}

}
