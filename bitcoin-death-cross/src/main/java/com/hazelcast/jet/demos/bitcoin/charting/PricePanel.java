package com.hazelcast.jet.demos.bitcoin.charting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.time.LocalDate;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import com.hazelcast.jet.demos.bitcoin.MyConstants;

/**
 * <p>This class is a time-series panel to
 * display on screen.
 * </p>
 * <p>The time-series in this case are the prices
 * of Bitcoin in US Dollars produced by the Jet
 * job.
 * </p>
 * <p>The current price is plotted in blue, the
 * 50-point moving average in red, the 200-point
 * in magenta. All points are plotted on the same
 * chart, which is resized as necessary.
 * </p>
 */
@SuppressWarnings("serial")
public class PricePanel extends JPanel {

    private final TimeSeries[] timeSeries;
	
	/**
	 * <p>Create a time-series panel with the X-Axis for
	 * the date and the Y-Axis for the price of Bitcoin
	 * </p>
	 */
	public PricePanel() {
	    TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection();

		// Initialise all three cached prices to zero
	    this.timeSeries = new TimeSeries[MyConstants.CHART_LINES.length];
		for (int i=0 ; i<MyConstants.CHART_LINES.length; i++) {
			this.timeSeries[i] = new TimeSeries(MyConstants.CHART_LINES[i]);
			timeSeriesCollection.addSeries(this.timeSeries[i]);
		}

	    // Create a chart with the title and axis labels
	    JFreeChart jFreeChart = ChartFactory.createTimeSeriesChart(
	    		MyConstants.CHART_TITLE,
	    		MyConstants.CHART_X_AXIS_LEGEND,
	    		MyConstants.CHART_Y_AXIS_LEGEND,
                timeSeriesCollection);

	    // Configure Y-Axis on chart, the BTC/USD exchange rate, up to $20,000
	    ValueAxis yAxis = jFreeChart.getXYPlot().getRangeAxis();

	    /* Set the y-axis upper bound to a fixed $20,000 dollars.
	     * If you'd prefer dynamic resizing, use autoRange
	     * instead.
	     */
	    // yAxis.setAutoRange(true);
	    yAxis.setRange(new Range(0d, 20_000d));
	    
	    // Allow scrolling
	    jFreeChart.getXYPlot().setDomainPannable(true);
	    jFreeChart.getXYPlot().setRangePannable(true);

	    // Colours for average, 50-point, 200-point respectively
	    XYPlot xyPlot = jFreeChart.getXYPlot();
	    XYItemRenderer xYItemRenderer = xyPlot.getRenderer();
	    xYItemRenderer.setSeriesPaint(0, Color.BLUE);
	    xYItemRenderer.setSeriesPaint(1, Color.RED);
	    xYItemRenderer.setSeriesPaint(2, Color.MAGENTA);

	    // Add the chart to the panel
	    this.setLayout(new BorderLayout());
	    this.add(new ChartPanel(jFreeChart));
	}
	
	/**
	 * <p>Update the panel with a new price on a date, from
	 * current, 50-point or 200-point averages.
	 * </p>
	 * 
	 * @param name Current, 50 point average or 200 point average
	 * @param day The day that price occurred
	 * @param rate In US Dollars, the price of Bitcoin
	 */
    public void update(String name, LocalDate localDate, double rate) {
		Day day = new Day(localDate.getDayOfMonth(), localDate.getMonthValue(), localDate.getYear());

    	// Add the relevant price into the time series array
    	for (int i=0 ; i < MyConstants.CHART_LINES.length; i++) {
    		if (MyConstants.CHART_LINES[i].equals(name)) {
				this.timeSeries[i].addOrUpdate(new TimeSeriesDataItem(day, rate));
     		}
    	}
    }

}
