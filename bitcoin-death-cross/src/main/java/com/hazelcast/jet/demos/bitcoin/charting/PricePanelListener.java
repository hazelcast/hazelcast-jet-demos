package com.hazelcast.jet.demos.bitcoin.charting;

import java.time.LocalDate;

import javax.swing.JFrame;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.jet.demos.bitcoin.MyConstants;
import com.hazelcast.jet.demos.bitcoin.alerting.MyTopicListener;
import com.hazelcast.jet.demos.bitcoin.domain.Price;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

/**
 * <p>Connect {@link com.hazelcast.core.IMap IMap}
 * change history into a graph plot.
 * </p>
 * <p>Each time a price is written into the map,
 * Hazelcast passes this class an event object
 * containing the event details. Pass this event's
 * details to the {@link PricePanel} to display.
 * </p>
 * <p>It's the same price keys that keep being
 * updated, so plotting with time gives us a graph
 * of the price changing over time.
 * </p>
 */
public class PricePanelListener implements 
	EntryAddedListener<String, Price>,
	EntryUpdatedListener<String, Price> {

	private static PricePanel pricePanel = null;
	private MyTopicListener myTopicListener;
	
	public PricePanelListener(MyTopicListener arg0) {
		this.myTopicListener = arg0;
	}

	/**
	 * <p>Create the graph plot on screen. This is
	 * invoked when the first data point arrives, so
	 * we create the graph once there is something to
	 * plot on it, rather than blank.
	 * </p>
	 */
	public void activateDisplay() {
		pricePanel = new PricePanel(this.myTopicListener);

		JFrame frame = new JFrame(MyConstants.PANEL_TITLE);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(pricePanel);
		frame.pack();
		frame.setVisible(true);
	}

	// Plot initial and subsequent data updates
	@Override
	public void entryAdded(EntryEvent<String, Price> entryEvent) {
		this.handle(entryEvent);
	}
	@Override
	public void entryUpdated(EntryEvent<String, Price> entryEvent) {
		this.handle(entryEvent);
	}
	
	
	/**
	 * <p>An event is produced each time a price is written
	 * into an {@link com.hazelcast.core.IMap IMap}. For each
	 * of these, add it to the graph, so producing a plot of
	 * the changes to the map.
	 * </p>
	 *
	 * @param entryEvent Produced by Jet writing into an IMap
	 */
	public void handle(EntryEvent<String, Price> entryEvent) {

		// Initialise the panel on first use
		synchronized (this) {
			if (pricePanel == null) {
				this.activateDisplay();
			}
		}

		// Extract the fields, and simplify the rate for plotting
		String priceKey = entryEvent.getKey();
		LocalDate day = entryEvent.getValue().getLocalDate();
		double rate = entryEvent.getValue().getRate().doubleValue();
		
		// Add this price to the chart
		pricePanel.update(priceKey, day, rate);
	}

}
