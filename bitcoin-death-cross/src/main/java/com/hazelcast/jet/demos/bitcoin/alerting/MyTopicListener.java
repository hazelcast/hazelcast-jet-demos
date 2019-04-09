package com.hazelcast.jet.demos.bitcoin.alerting;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Handle a message received from Hazelcast
 * {@link com.hazelcast.core.ITopic ITopic}.
 * Here we just display the content.
 * </p>
 */
@Slf4j
public class MyTopicListener implements MessageListener<Object> {

	private static final String NEWLINE = System.getProperty("line.separator");
	
	@Override
	public void onMessage(Message<Object> message) {
		log.warn("ITopic alert"
				 + NEWLINE
				 + "*********************************************************"
				 + NEWLINE
				 + "Topic '{}' : {}"
				 + NEWLINE
				 + "*********************************************************",
				 message.getSource(),
				 message.getMessageObject()
				);
	}

}
