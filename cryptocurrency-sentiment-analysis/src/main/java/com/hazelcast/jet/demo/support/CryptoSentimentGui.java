package com.hazelcast.jet.demo.support;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;

import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class CryptoSentimentGui {
    private static final int WINDOW_X = 200;
    private static final int WINDOW_Y = 0;
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 500;

    private final JFrame frame = new JFrame();
    private final DefaultCategoryDataset sentimentDataset = new DefaultCategoryDataset();
    private final DefaultCategoryDataset mentionsDataset = new DefaultCategoryDataset();

    public CryptoSentimentGui(
            IMap<Tuple2<CoinType, WinSize>, Tuple2<Double, Long>> jetResults
    ) {
        for (WinSize winSize : WinSize.values()) {
            for (CoinType coinType : CoinType.values()) {
                sentimentDataset.addValue(0.0, coinType, winSize);
                mentionsDataset.addValue(0.0, coinType, winSize);
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
            Tuple2<Double, Long> metrics = event.getValue();
            double sentimentScore = metrics.f0();
            double mentionsPerMinute = metrics.f1() / ((double) winSize.durationMillis() / MINUTES.toMillis(1));
            sentimentDataset.addValue(sentimentScore, coinType, winSize);
            mentionsDataset.addValue(mentionsPerMinute, coinType, winSize);
            frame.repaint();
        });
    }

    private void startGui() {
        JFreeChart sentimentChart =
                chart("Cryptocurrency Sentiment in Tweets", "Sentiment Score", sentimentDataset, -1.0, 1.0);
        JFreeChart mentionsChart =
                chart("Mentions of Cryptocurrency in Tweets", "Tweets per Minute", mentionsDataset, 0, 100);

        JFrame frame = new JFrame();
        frame.setBackground(Color.WHITE);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setTitle("Hazelcast Jet Cryptocurrency Popularity and Sentiment");
        frame.setBounds(WINDOW_X, WINDOW_Y, WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));
        frame.add(new ChartPanel(mentionsChart));
        frame.add(new ChartPanel(sentimentChart));
        frame.setVisible(true);
    }

    private JFreeChart chart(
            String title, String valueAxisLabel, CategoryDataset dataset,
            double rangeMin, double rangeMax
    ) {
        JFreeChart chart = ChartFactory.createBarChart(
                title, "Time Range", valueAxisLabel, dataset,
                PlotOrientation.VERTICAL, true, true, false);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.DARK_GRAY);
        plot.setRangeGridlinePaint(Color.DARK_GRAY);
        plot.getRangeAxis().setRange(rangeMin, rangeMax);
        return chart;
    }
}
