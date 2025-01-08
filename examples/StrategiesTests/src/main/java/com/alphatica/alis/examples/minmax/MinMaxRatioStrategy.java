package com.alphatica.alis.examples.minmax;

import com.alphatica.alis.charting.Chart;
import com.alphatica.alis.charting.LineChartData;
import com.alphatica.alis.data.market.MarketName;
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
import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.tools.java.CollectionsTools.arrayList;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.PERCENTAGE;

public class MinMaxRatioStrategy extends Strategy {
	private static final double UP_RATIO = 0.505;
	private static final double DOWN_RATIO = 0.495;
	private final MarketName wig = new MarketName("wig");
	private final MinMax minMax = new MinMax(170);
	LineChartData<String> wigLine = new LineChartData<>("WIG");
	LineChartData<String> navLine = new LineChartData<>("Equity");

	int signals = 0;

	@Override
	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		TimeMarketData wigNow = data.get(wig);
		if (wigNow != null) {
			wigLine.addPoint(wigNow.getTime().toString(), wigNow.getData(CLOSE, 0));
			navLine.addPoint(wigNow.getTime().toString(), account.getNAV());
			double ratio = getRatio(data);
			if (account.getPosition(wig) == null && ratio > UP_RATIO) {
				signals++;
				return arrayList(new Order(wig, BUY, PERCENTAGE, 100, 1.0));
			}
			if (account.getPosition(wig) != null && ratio < DOWN_RATIO) {
				signals++;
				return arrayList(new Order(wig, SELL, PERCENTAGE, 100, 1.0));
			}
		}
		return arrayList();
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
		System.out.println("Signals: " + signals);
	}

	double getRatio(TimeMarketDataSet data) {
		double ups = 0;
		double downs = 0;

		for (TimeMarketData marketData : data.listMarkets(STOCKS)) {
			double minMaxNow = minMax.calculate(marketData);
			if (minMaxNow > 0) {
				ups++;
			}
			if (minMaxNow < 0) {
				downs++;
			}
		}
		double total = ups + downs;
		if (total == 0) {
			return Double.NaN;
		} else {
			return ups / (ups + downs);
		}
	}
}
