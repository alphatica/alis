package com.alphatica.alis.examples.ath;

import com.alphatica.alis.charting.Chart;
import com.alphatica.alis.charting.LineChartData;
import com.alphatica.alis.condition.AllTimeHigh;
import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.DoubleArraySlice;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.LONG;
import static com.alphatica.alis.trading.order.Direction.EXIT;
import static com.alphatica.alis.trading.order.OrderSize.PROPORTION;

public class AthPlusTimePyramidStrategy extends Strategy {
	private static final int SESSIONS = 44;
	private static final double RATIO = 0.01;
	private final Map<MarketName, Integer> counters = new HashMap<>();
	private final Condition allTimeHigh = new AllTimeHigh();
	private final LineChartData<String> equityLine = new LineChartData<>();

	@Override
	public List<Order> afterClose(TimeMarketDataSet allData, Account account) {
		equityLine.addPoint(allData.listMarkets(STOCKS).getFirst().getTime().toString(), account.getNAV());
		List<Order> orders = new ArrayList<>();
		for (TimeMarketData market : allData.listMarkets(STOCKS)) {
			MarketName marketName = market.getMarketName();
			counters.putIfAbsent(marketName, 0);
			double currentValue = account.getPositionValue(marketName);
			if (allTimeHigh.matches(market, allData)) {
				counters.put(marketName, 0);
				checkBuy(marketName, currentValue, orders, account.getNAV(), market.getLayer(CLOSE));
			} else {
				int counterNow = counters.get(marketName) + 1;
				counters.put(marketName, counterNow);
				checkSell(marketName, currentValue, orders, counterNow);
			}
		}
		return orders;
	}

	private double orderPriority(DoubleArraySlice closes) {
		double priority;
		if (closes.size() > SESSIONS) {
			priority = closes.get(0) / closes.get(SESSIONS);
		} else {
			priority = closes.get(0) / closes.get(closes.size() - 1);
		}
		return priority;
	}


	private void checkSell(MarketName marketName, double currentValue, List<Order> orders, int counterNow) {
		if (counterNow > SESSIONS && currentValue > 0.0) {
			orders.add(new Order(marketName, EXIT, PROPORTION, 1.0, 0.0));
		}
	}

	private void checkBuy(MarketName marketName, double currentValue, List<Order> orders, double nav, DoubleArraySlice closes) {
		if (currentValue < 0.1 * nav) {
			double priority = orderPriority(closes);
			orders.add(new Order(marketName, LONG, PROPORTION, RATIO, priority));
		}
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	@Override
	public void finished(Account account) {
		Chart<String> chart = new Chart<>();
		equityLine.setConnectPoints(true);
		chart.addDataLines(List.of(equityLine));
		chart.setTitle("Buy at ATH + pyramid, hold for 2 months, ");
		chart.setCopyright("Alphatica.com");
		try {
			chart.createImage(new File("athTimePyramid.png"));
		} catch (IOException e) {
			System.out.println("Unable to create chart: " + e.getMessage());
		}
	}
}
