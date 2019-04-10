package com.hazelcast.jet.demos.bitcoin.alerting;

import javax.swing.JTextArea;

import org.springframework.stereotype.Component;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Handle a message received from Hazelcast
 * {@link com.hazelcast.core.ITopic ITopic}.
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
	    this.setText(sb.toString());

	}

}
