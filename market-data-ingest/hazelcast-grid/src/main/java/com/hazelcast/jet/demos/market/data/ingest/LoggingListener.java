package com.hazelcast.jet.demos.market.data.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

@SuppressWarnings("rawtypes")
public class LoggingListener implements EntryAddedListener, EntryUpdatedListener {

	private static Logger log = LoggerFactory.getLogger(LoggingListener.class);
	
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
