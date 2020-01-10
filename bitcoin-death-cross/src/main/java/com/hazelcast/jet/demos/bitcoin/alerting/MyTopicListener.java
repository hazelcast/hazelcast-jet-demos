package com.hazelcast.jet.demos.bitcoin.alerting;

import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.swing.*;

/**
 * <p>Handle a message received from Hazelcast
 * {@link com.hazelcast.topic.ITopic ITopic}.
 * Here we just display the content.
 * </p>
 */
@SuppressWarnings("serial")
@Component
@Slf4j
public class MyTopicListener extends JTextArea implements MessageListener<Object> {

	private static final String NEWLINE = System.getProperty("line.separator");
	private static final String FOUR_SPACES = "    ";
	
	@Override
	public void onMessage(Message<Object> message) {
		String outputMessage = 
				String.format("Topic '%s' : %s", message.getSource(), message.getMessageObject());
		
		String[] output = new String[] {
				NEWLINE,
				FOUR_SPACES + "***************************************************************"
					+ FOUR_SPACES,
				FOUR_SPACES + outputMessage + FOUR_SPACES,
				FOUR_SPACES + "***************************************************************"
					+ FOUR_SPACES,
		};
		
		StringBuilder sb = new StringBuilder();
		for (String line : output) {
			sb.append(line).append(NEWLINE);
		}
		log.warn(NEWLINE + sb.toString() + NEWLINE);
		
		/* MyTopicListener is a JTextArea.
		 */
	    this.setVisible(true);
	    String before = this.getText();
	    if (before == null) {
	        this.setText(sb.toString());
	    } else {
	        this.setText(before + sb.toString());
	    }

	}

}
