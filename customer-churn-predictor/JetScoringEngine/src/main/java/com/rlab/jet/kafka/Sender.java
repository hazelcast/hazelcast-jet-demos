/**
 * @author Riaz Mohammed
 *
 * 
 */

package com.rlab.jet.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rlab.kafka.message.KMessage;

@Service
public class Sender {
 
  static int count=0;
  private static final Logger LOGGER = LoggerFactory.getLogger(Sender.class);
  ObjectMapper jom = new ObjectMapper();
  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

 /* public void send(String topic, String payload) {
	  String key="Key-"+(++count);
    LOGGER.info("sending payload='{}' to topic='{}' with Key='{}'", payload, topic,key);
   //kafkaTemplate.send(topic,key ,payload);
    kafkaTemplate.send(topic,payload);
  } */
  
  public void send(String topic, KMessage payload) {
	  String key="Key-"+(++count);
    LOGGER.info("sending payload='{}' to topic='{}' with Key='{}'", payload, topic,key);
   //kafkaTemplate.send(topic,key ,payload);
    try {
		kafkaTemplate.send(topic,Integer.toString(payload.getId()),jom.writeValueAsString(payload));
	} catch (JsonProcessingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }
  
  /*public void sendRecord(String topic, String payload) {
	    LOGGER.info("sending payload='{}' to topic='{}'", payload, topic);
	    try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
                producer.send(new ProducerRecord<>("t1", "t1-" + i, payload));
            System.out.println("Published " + MESSAGE_COUNT_PER_TOPIC + " messages to topic t1");
            System.out.println("Published " + MESSAGE_COUNT_PER_TOPIC + " messages to topic t2");
        }
	  }*/
}