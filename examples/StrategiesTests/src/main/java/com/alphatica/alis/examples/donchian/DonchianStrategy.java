package com.alphatica.alis.examples.donchian;

import com.alphatica.alis.charting.Chart;
import com.alphatica.alis.charting.LineChartData;
import com.alphatica.alis.condition.HighestClose;
import com.alphatica.alis.condition.LowestClose;
import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.FloatArraySlice;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.PERCENTAGE;

public class DonchianStrategy extends Strategy {
	private final HighestClose highestClose;
	private final LowestClose lowestClose;
	private final boolean highestGrowthFirst;
	private final boolean drawChart;
	private final LineChartData<String> equityLine = new LineChartData<>();
	private final LineChartData<String> wigLine = new LineChartData<>();

	public DonchianStrategy(int buyLength, int sellLength, boolean highestGrowthFirst, boolean drawChart) {
		this.highestClose = new HighestClose(buyLength);
		this.lowestClose = new LowestClose(sellLength);
		this.highestGrowthFirst = highestGrowthFirst;
		this.drawChart = drawChart;
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet allData, Account account) {
		if (drawChart) {
			String time = allData.getTime().toString();
			equityLine.addPoint(time, account.getNAV());
			TimeMarketData wig = allData.get(new MarketName("wig"));
			if (wig == null) {
				return Collections.emptyList();
			}
			wigLine.addPoint(time, wig.getData(Layer.CLOSE, 0));
		}
		List<Order> orders = new ArrayList<>();
		for (TimeMarketData marketData : allData.listMarkets(STOCKS)) {
			checkStock(account, marketData, allData, orders);
		}
		return orders;
	}

	private void checkStock(Account account, TimeMarketData marketData, TimeMarketDataSet allData, List<Order> orders) {
		double currentValue = account.getPositionValue(marketData.getMarketName());
		if (currentValue > 0 && lowestClose.matches(marketData, allData)) {
			orders.add(new Order(marketData.getMarketName(), SELL, PERCENTAGE, 100, 0.0));
		}
		if (currentValue < account.getNAV() * 0.05 && highestClose.matches(marketData, allData)) {
			double priority = orderPriority(marketData.getLayer(Layer.CLOSE));
			orders.add(new Order(marketData.getMarketName(), BUY, PERCENTAGE, 5, priority));
		}
	}

	private double orderPriority(FloatArraySlice closes) {
		double priority;
		if (closes.size() > 250) {
			priority = closes.get(0) / closes.get(250);
		} else {
			priority = closes.get(0) / closes.get(closes.size() - 1);
		}
		if (!highestGrowthFirst) {
			priority = 1 / priority;
		}
		return priority;
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	@Override
	public void finished(Account account) {
		if (drawChart) {
			Chart<String> chart = new Chart<>();
			equityLine.setConnectPoints(true);
			equityLine.setName("Equity");
			wigLine.setConnectPoints(true);
			wigLine.setName("WIG");
			chart.addDataLines(List.of(equityLine, wigLine));
			chart.setTitle("Donchian channel breakout");
			chart.setCopyright("Alphatica.com");
			try {
				chart.createImage(new File("donchianBreakout.png"));
			} catch (IOException e) {
				System.out.println("Unable to create chart: " + e.getMessage());
			}
		}
	}

}
