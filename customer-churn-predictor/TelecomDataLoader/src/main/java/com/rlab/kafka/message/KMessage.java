/**
 * @author Riaz Mohammed
 *
 * 
 */

package com.rlab.kafka.message;

import java.util.HashMap;
import java.util.Map;

public class KMessage  {
	private int  id;
	private int messageType;
	private Map<String,Object> attributes = new HashMap<String,Object>();
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	// 0 = request  1 = result
	public int getMessageType() {
		return messageType;
	}
	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}
	public Map<String, ?> getAttributes() {
		return attributes;
	}
	public Object getAttribute(String key) {
		return attributes.get(key);
	}
	public void setAttribute(String key,Object val) {
		this.attributes.put(key, val);
	}
	@Override
	public String toString() {
		return "KMessage [id=" + id + ", messageType=" + messageType + ", attributes=" + attributes + "]";
	}
	
	
}
