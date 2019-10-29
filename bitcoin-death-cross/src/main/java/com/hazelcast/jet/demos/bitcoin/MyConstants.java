package com.hazelcast.jet.demos.bitcoin;

import java.time.LocalDate;

import com.hazelcast.jet.demos.bitcoin.job.MovingAverage;

/**
 * <p>Define some constants to make the coding clearer.
 * </p>
 */
public class MyConstants {

	// For ordering execution of tasks
	public static final int PRIORITY_ZERO = 0;
	public static final int PRIORITY_ONE = 1;
	public static final int PRIORITY_TWO = 2;
	public static final int PRIORITY_THREE = 3;
	public static final int PRIORITY_FOUR = 4;
	
	// Jet job name
	public static final String JOB_NAME = MovingAverage.class.getSimpleName();
	
	// Delay between prices on price feed
	public static final long PRICE_DELAY_MS = 50L;
	
	// Currency pair for Bitcoin/US Dollar
	public static final String BTCUSD = "BTCUSD";
	
	// IMap names
	public static final String IMAP_NAME_PRICES_IN = "prices-in";
	public static final String IMAP_NAME_PRICES_OUT_BTCUSD = BTCUSD;

	// ITopic names
    public static final String ITOPIC_NAME_ALERT = "alert";

	// Keys of data output by Jet job
    public static final String KEY_CURRENT = "Current";
    public static final String KEY_50_POINT = "50 Point";
    public static final String KEY_200_POINT = "200 Point";

    // For the price plotting
    public static final String PANEL_TITLE = "Analysis " + LocalDate.now()
    	+ " (THIS IS NOT INVESTMENT ADVICE!)";
    public static final String CHART_TITLE = "Bitcoin v US Dollar";
    public static final String CHART_X_AXIS_LEGEND = "Date";
    public static final String CHART_Y_AXIS_LEGEND = "US$";
    public static final String[] CHART_LINES = { 
                    KEY_CURRENT, KEY_50_POINT, KEY_200_POINT
                    };
    
    // For cluster formation delay
    public static final int STARTUP_DELAY_SECONDS = 10;
}
