package com.hazelcast.jet.demo.support;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class CryptoSentimentGui {
    private static final int WINDOW_X = 400;
    private static final int WINDOW_Y = 0;
    private static final int WINDOW_WIDTH = 700;
    private static final int WINDOW_HEIGHT = 500;

    private final JFrame frame = new JFrame();
    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    public CryptoSentimentGui(
            IMap<Tuple2<CoinType, WinSize>, Tuple2<Double, Long>> jetResults
    ) {
        for (WinSize winSize : WinSize.values()) {
            for (CoinType coinType : CoinType.values()) {
                dataset.addValue(0.0, coinType, winSize);
            }
        }

        jetResults.addEntryListener(
                (EntryAddedListener<Tuple2<CoinType, WinSize>, Tuple2<Double, Long>>) this::onMapEvent, true);
        jetResults.addEntryListener(
                (EntryUpdatedListener<Tuple2<CoinType, WinSize>, Tuple2<Double, Long>>) this::onMapEvent, true);
        EventQueue.invokeLater(this::startGui);
    }

    private void onMapEvent(EntryEvent<Tuple2<CoinType, WinSize>, Tuple2<Double, Long>> event) {
        EventQueue.invokeLater(() -> {
            CoinType coinType = event.getKey().f0();
            WinSize winSize = event.getKey().f1();
            double sentimentScore = event.getValue().f0();
            dataset.addValue(sentimentScore, coinType, winSize);
            frame.repaint();
        });
    }

    private void startGui() {
        JFreeChart chart = ChartFactory.createBarChart(
                "Recent Cryptocurrency Sentiment in Tweets", "Time Range", "Sentiment Score", dataset,
                PlotOrientation.VERTICAL, true, true, false);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.DARK_GRAY);
        plot.setRangeGridlinePaint(Color.DARK_GRAY);
        plot.getRangeAxis().setRange(-1.0, 1.0);

        JFrame frame = new JFrame();
        frame.setBackground(Color.WHITE);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setTitle("Hazelcast Jet Cryptocurrency Sentiment Analysis");
        frame.setBounds(WINDOW_X, WINDOW_Y, WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setLayout(new BorderLayout());
        frame.add(new ChartPanel(chart));
        frame.setVisible(true);
    }
}
