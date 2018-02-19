package com.hazelcast.jet.demos.market.data.analyzer;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
public class LoggingListener implements EntryAddedListener, EntryUpdatedListener {

	@Override
	public void entryUpdated(EntryEvent arg0) {
		this.log(arg0);
	}
	@Override
	public void entryAdded(EntryEvent arg0) {
		this.log(arg0);
	}

	private void log(EntryEvent entryEvent) {
		log.info("{} event, map '{}', key '{}', new value '{}'",
				entryEvent.getEventType(),
				entryEvent.getName(),
				entryEvent.getKey(),
				entryEvent.getValue());
	}

}
