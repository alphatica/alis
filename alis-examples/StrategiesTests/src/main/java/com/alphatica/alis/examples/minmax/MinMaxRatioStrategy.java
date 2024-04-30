package com.alphatica.alis.examples.minmax;

import com.alphatica.alis.charting.Chart;
import com.alphatica.alis.charting.LineChartData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.MinMax;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.PROPORTION;

public class MinMaxRatioStrategy implements Strategy {
    private static final double UP_RATIO = 0.505;
    private static final double DOWN_RATIO = 0.495;
    private final MarketName wig = new MarketName("wig");
    private final MinMax minMax = new MinMax(170);
    LineChartData<String> wigLine = new LineChartData<>("WIG");
    LineChartData<String> navLine = new LineChartData<>("Equity");

    @Override
    public List<Order> afterClose(TimeMarketDataSet data, Account account) {
        TimeMarketData wigNow = data.get(wig);
        if (wigNow != null) {
            wigLine.addPoint(wigNow.getTime().toString(), wigNow.getData(CLOSE, 0));
            navLine.addPoint(wigNow.getTime().toString(), account.getNAV());
            double ratio = getRatio(data);
            if (account.getPosition(wig).isEmpty() && ratio > UP_RATIO) {
                return List.of(new Order(wig, BUY, PROPORTION, 1.0, 1.0));
            }
            if (account.getPosition(wig).isPresent() && ratio < DOWN_RATIO) {
                return List.of(new Order(wig, SELL, PROPORTION, 1.0, 1.0));
            }
        }
        return List.of();
    }

    @SuppressWarnings("java:S106") // Suppress warning about 'System.*.println'
    @Override
    public void finished(Account account) {
        Chart<String> chart = new Chart<>();
        wigLine.setConnectPoints(true);
        navLine.setConnectPoints(true);
        chart.addDataLines(List.of(wigLine, navLine));
        chart.setXName("Date");
        chart.setYName("Value");
        chart.setCopyright("Alphatica.com");
        chart.setTitle("Min-Max breadth WIG signals");
        try {
            chart.createImage(new File("minMax.png"));
        } catch (IOException e) {
            System.err.println("Unable to save image: " + e.getMessage());
        }
    }

    double getRatio(TimeMarketDataSet data) {
        int[] stats = {0, 0};
        data.listMarkets().forEach(marketData -> {
            if (marketData.getMarketType() == MarketType.STOCK) {
                minMax.calculate(marketData).ifPresent(v -> {
                    if (v > 0) {
                        stats[0]++;
                    } else {
                        stats[1]++;
                    }
                });
            }
        });
        return (double) stats[0] / (stats[1] + stats[0]);
    }
}
